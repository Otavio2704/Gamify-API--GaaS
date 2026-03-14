package com.gamifyapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamifyapi.dto.response.ActionResultResponse.LevelUpDetails;
import com.gamifyapi.dto.response.ActionResultResponse.StreakInfo;
import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.Player;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.entity.WebhookConfig;
import com.gamifyapi.entity.WebhookLog;
import com.gamifyapi.enums.WebhookEventType;
import com.gamifyapi.repository.WebhookConfigRepository;
import com.gamifyapi.repository.WebhookLogRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serviço assíncrono de disparo de webhooks.
 *
 * <p>Realiza até 3 tentativas com backoff exponencial (1s, 4s, 16s).
 * Retries são agendados via {@link ScheduledExecutorService} — nenhum thread
 * fica bloqueado aguardando o intervalo entre tentativas.
 * Cada requisição é assinada com HMAC-SHA256 no header {@code X-Gamify-Signature}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private static final int    MAX_TENTATIVAS = 3;
    private static final long[] BACKOFF_MS     = {1_000, 4_000, 16_000};

    private final WebhookConfigRepository webhookConfigRepository;
    private final WebhookLogRepository    webhookLogRepository;
    private final ObjectMapper            objectMapper;

    // Campo inicializado inline — Lombok não inclui no construtor
    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(
            2, r -> {
                Thread t = new Thread(r, "webhook-retry-" + r.hashCode());
                t.setDaemon(true);
                return t;
            });

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PreDestroy
    public void shutdown() {
        retryScheduler.shutdownNow();
    }

    // -------------------------------------------------------------------------

    @Async("webhookTaskExecutor")
    public void dispararAsync(Tenant tenant, Player player, LevelUpDetails levelUp,
                              List<Achievement> conquistas, StreakInfo streak) {
        if (levelUp != null && levelUp.happened()) {
            dispararEvento(tenant.getId(), WebhookEventType.LEVEL_UP,
                    buildPayloadLevelUp(player, levelUp));
        }
        if (conquistas != null) {
            for (Achievement conquista : conquistas) {
                dispararEvento(tenant.getId(), WebhookEventType.ACHIEVEMENT_UNLOCKED,
                        buildPayloadAchievement(player, conquista));
            }
        }
        if (streak != null && isStreakMilestone(streak.currentStreak())) {
            dispararEvento(tenant.getId(), WebhookEventType.STREAK_MILESTONE,
                    buildPayloadStreak(player, streak));
        }
    }

    // -------------------------------------------------------------------------

    private void dispararEvento(Long tenantId, WebhookEventType tipo,
                                Map<String, Object> payload) {
        List<WebhookConfig> webhooks = webhookConfigRepository
                .findAllByTenantIdAndEventTypeAndActiveTrue(tenantId, tipo);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Erro ao serializar payload do webhook {}: {}", tipo, e.getMessage());
            return;
        }

        for (WebhookConfig config : webhooks) {
            tentarEnvio(config.getId(), tipo, json, 0);
        }
    }

    /**
     * Executa uma tentativa de envio. Em caso de falha, agenda o retry no
     * {@link #retryScheduler} sem bloquear o thread atual.
     */
    private void tentarEnvio(Long configId, WebhookEventType tipo,
                              String json, int tentativa) {
        if (tentativa >= MAX_TENTATIVAS) {
            log.error("Webhook {} falhou após {} tentativas (configId={})",
                    tipo, MAX_TENTATIVAS, configId);
            return;
        }

        WebhookConfig config = webhookConfigRepository.findById(configId).orElse(null);
        if (config == null) {
            log.warn("WebhookConfig {} não encontrada, abortando envio de {}", configId, tipo);
            return;
        }

        boolean sucesso      = false;
        Integer statusResposta = null;

        try {
            String assinatura = gerarAssinatura(json, config.getSecretKey());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type",        "application/json")
                    .header("X-Gamify-Signature",  "sha256=" + assinatura)
                    .header("X-Gamify-Event",       tipo.name())
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resposta = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            statusResposta = resposta.statusCode();
            sucesso        = statusResposta >= 200 && statusResposta < 300;

        } catch (Exception e) {
            log.warn("Erro ao enviar webhook {} para {} (tentativa {}/{}): {}",
                    tipo, config.getUrl(), tentativa + 1, MAX_TENTATIVAS, e.getMessage());
        }

        salvarLog(config, tipo, json, statusResposta, sucesso, tentativa + 1);

        if (sucesso) {
            log.debug("Webhook {} entregue para {} (tentativa {})",
                    tipo, config.getUrl(), tentativa + 1);
            return;
        }

        // Agenda o próximo retry sem bloquear o thread
        int proxima = tentativa + 1;
        log.warn("Webhook {} falhou (tentativa {}/{}), retry em {}ms",
                tipo, tentativa + 1, MAX_TENTATIVAS, BACKOFF_MS[tentativa]);

        retryScheduler.schedule(
                () -> tentarEnvio(configId, tipo, json, proxima),
                BACKOFF_MS[tentativa],
                TimeUnit.MILLISECONDS
        );
    }

    private void salvarLog(WebhookConfig config, WebhookEventType tipo, String payload,
                           Integer status, boolean sucesso, int tentativa) {
        try {
            webhookLogRepository.save(WebhookLog.builder()
                    .webhookConfig(config)
                    .eventType(tipo)
                    .payload(payload)
                    .responseStatus(status)
                    .success(sucesso)
                    .attemptCount(tentativa)
                    .sentAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("Falha ao salvar log do webhook: {}", e.getMessage());
        }
    }

    private String gerarAssinatura(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Erro ao gerar assinatura HMAC-SHA256: {}", e.getMessage());
            return "";
        }
    }

    private boolean isStreakMilestone(int streak) {
        return streak > 0 && streak % 7 == 0;
    }

    // -------------------------------------------------------------------------
    // Builders de payload
    // -------------------------------------------------------------------------

    private Map<String, Object> buildPayloadLevelUp(Player player, LevelUpDetails levelUp) {
        Map<String, Object> m = new HashMap<>();
        m.put("event",         WebhookEventType.LEVEL_UP.name());
        m.put("playerId",      player.getExternalId());
        m.put("newLevel",      levelUp.newLevel());
        m.put("previousLevel", levelUp.previousLevel());
        m.put("totalXp",       player.getTotalXp());
        m.put("timestamp",     Instant.now().toString());
        return m;
    }

    private Map<String, Object> buildPayloadAchievement(Player player, Achievement conquista) {
        Map<String, Object> m = new HashMap<>();
        m.put("event",           WebhookEventType.ACHIEVEMENT_UNLOCKED.name());
        m.put("playerId",        player.getExternalId());
        m.put("achievementCode", conquista.getCode());
        m.put("achievementName", conquista.getName());
        m.put("xpReward",        conquista.getXpReward());
        m.put("timestamp",       Instant.now().toString());
        return m;
    }

    private Map<String, Object> buildPayloadStreak(Player player, StreakInfo streak) {
        Map<String, Object> m = new HashMap<>();
        m.put("event",         WebhookEventType.STREAK_MILESTONE.name());
        m.put("playerId",      player.getExternalId());
        m.put("currentStreak", streak.currentStreak());
        m.put("longestStreak", streak.longestStreak());
        m.put("timestamp",     Instant.now().toString());
        return m;
    }
}
