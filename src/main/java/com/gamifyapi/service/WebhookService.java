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

/**
 * Serviço assíncrono de disparo de webhooks.
 *
 * <p>Realiza até 3 tentativas com backoff exponencial (1s, 4s, 16s).
 * Assina cada requisição com HMAC-SHA256 no header {@code X-Gamify-Signature}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private static final int MAX_TENTATIVAS = 3;
    private static final long[] BACKOFF_MS = {1_000, 4_000, 16_000};

    private final WebhookConfigRepository webhookConfigRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Dispara todos os webhooks relevantes de forma assíncrona.
     */
    @Async("webhookTaskExecutor")
    public void dispararAsync(Tenant tenant, Player player, LevelUpDetails levelUp,
                              List<Achievement> conquistas, StreakInfo streak) {
        if (levelUp != null && levelUp.happened()) {
            dispararEvento(tenant, player, WebhookEventType.LEVEL_UP, buildPayloadLevelUp(player, levelUp));
        }
        if (conquistas != null) {
            for (Achievement conquista : conquistas) {
                dispararEvento(tenant, player, WebhookEventType.ACHIEVEMENT_UNLOCKED,
                    buildPayloadAchievement(player, conquista));
            }
        }
        if (streak != null && isStreakMilestone(streak.currentStreak())) {
            dispararEvento(tenant, player, WebhookEventType.STREAK_MILESTONE, buildPayloadStreak(player, streak));
        }
    }

    // -------------------------------------------------------------------------
    // Métodos privados
    // -------------------------------------------------------------------------

    private void dispararEvento(Tenant tenant, Player player,
                                WebhookEventType tipo, Map<String, Object> payload) {
        List<WebhookConfig> webhooks = webhookConfigRepository
            .findAllByTenantIdAndEventTypeAndActiveTrue(tenant.getId(), tipo);

        for (WebhookConfig config : webhooks) {
            enviarComRetentativa(config, tipo, payload);
        }
    }

    private void enviarComRetentativa(WebhookConfig config, WebhookEventType tipo,
                                      Map<String, Object> payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Erro ao serializar payload do webhook: {}", e.getMessage());
            return;
        }

        for (int tentativa = 0; tentativa < MAX_TENTATIVAS; tentativa++) {
            if (tentativa > 0) {
                aguardar(BACKOFF_MS[tentativa - 1]);
            }

            Integer statusResposta = null;
            boolean sucesso = false;

            try {
                String assinatura = gerarAssinatura(json, config.getSecretKey());

                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("X-Gamify-Signature", "sha256=" + assinatura)
                    .header("X-Gamify-Event", tipo.name())
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

                HttpResponse<String> resposta = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

                statusResposta = resposta.statusCode();
                sucesso = statusResposta >= 200 && statusResposta < 300;

                salvarLog(config, tipo, json, statusResposta, sucesso, tentativa + 1);

                if (sucesso) {
                    log.debug("Webhook {} enviado com sucesso para {} (tentativa {})",
                        tipo, config.getUrl(), tentativa + 1);
                    return;
                }

                log.warn("Webhook {} retornou HTTP {} (tentativa {}/{})",
                    tipo, statusResposta, tentativa + 1, MAX_TENTATIVAS);

            } catch (Exception e) {
                log.warn("Erro ao enviar webhook {} para {} (tentativa {}/{}): {}",
                    tipo, config.getUrl(), tentativa + 1, MAX_TENTATIVAS, e.getMessage());
                salvarLog(config, tipo, json, statusResposta, false, tentativa + 1);
            }
        }

        log.error("Webhook {} falhou após {} tentativas para {}",
            tipo, MAX_TENTATIVAS, config.getUrl());
    }

    private void salvarLog(WebhookConfig config, WebhookEventType tipo, String payload,
                           Integer status, boolean sucesso, int tentativa) {
        try {
            WebhookLog wl = WebhookLog.builder()
                .webhookConfig(config)
                .eventType(tipo)
                .payload(payload)
                .responseStatus(status)
                .success(sucesso)
                .attemptCount(tentativa)
                .sentAt(Instant.now())
                .build();
            webhookLogRepository.save(wl);
        } catch (Exception e) {
            log.error("Falha ao salvar log do webhook: {}", e.getMessage());
        }
    }

    private String gerarAssinatura(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            log.error("Erro ao gerar assinatura HMAC-SHA256: {}", e.getMessage());
            return "";
        }
    }

    private void aguardar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // Builders de payload
    // -------------------------------------------------------------------------

    private Map<String, Object> buildPayloadLevelUp(Player player, LevelUpDetails levelUp) {
        Map<String, Object> m = new HashMap<>();
        m.put("event", WebhookEventType.LEVEL_UP.name());
        m.put("playerId", player.getExternalId());
        m.put("newLevel", levelUp.newLevel());
        m.put("previousLevel", levelUp.previousLevel());
        m.put("totalXp", player.getTotalXp());
        m.put("timestamp", Instant.now().toString());
        return m;
    }

    private Map<String, Object> buildPayloadAchievement(Player player, Achievement conquista) {
        Map<String, Object> m = new HashMap<>();
        m.put("event", WebhookEventType.ACHIEVEMENT_UNLOCKED.name());
        m.put("playerId", player.getExternalId());
        m.put("achievementCode", conquista.getCode());
        m.put("achievementName", conquista.getName());
        m.put("xpReward", conquista.getXpReward());
        m.put("timestamp", Instant.now().toString());
        return m;
    }

    /** Considera múltiplos de 7 dias como marcos de streak. */
    private boolean isStreakMilestone(int streak) {
        return streak > 0 && streak % 7 == 0;
    }

    private Map<String, Object> buildPayloadStreak(Player player, StreakInfo streak) {
        Map<String, Object> m = new HashMap<>();
        m.put("event", WebhookEventType.STREAK_MILESTONE.name());
        m.put("playerId", player.getExternalId());
        m.put("currentStreak", streak.currentStreak());
        m.put("longestStreak", streak.longestStreak());
        m.put("timestamp", Instant.now().toString());
        return m;
    }
}
