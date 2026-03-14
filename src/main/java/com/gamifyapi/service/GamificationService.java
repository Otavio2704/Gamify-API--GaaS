package com.gamifyapi.service;

import com.gamifyapi.achievement.AchievementEngine;
import com.gamifyapi.dto.request.ProcessActionRequest;
import com.gamifyapi.dto.response.ActionResultResponse;
import com.gamifyapi.dto.response.ActionResultResponse.LevelUpDetails;
import com.gamifyapi.dto.response.ActionResultResponse.StreakInfo;
import com.gamifyapi.dto.response.AchievementUnlockedResponse;
import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.ActionDefinition;
import com.gamifyapi.entity.Player;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.exception.RecursoNaoEncontradoException;
import com.gamifyapi.repository.ActionDefinitionRepository;
import com.gamifyapi.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestrador central do fluxo de processamento de ação.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>Busca/cria player</li>
 *   <li>Valida cooldown</li>
 *   <li>Concede XP</li>
 *   <li>Verifica level up</li>
 *   <li>Atualiza streak</li>
 *   <li>Avalia conquistas (pode gerar XP bônus → re-verifica level up)</li>
 *   <li>Atualiza posição no ranking</li>
 *   <li>Dispara webhooks assíncronos</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final PlayerService playerService;
    private final CooldownService cooldownService;
    private final XpService xpService;
    private final LevelService levelService;
    private final StreakService streakService;
    private final AchievementEngine achievementEngine;
    private final RankingService rankingService;
    private final WebhookService webhookService;
    private final ActionDefinitionRepository actionDefinitionRepository;

    /**
     * Processa uma ação de um player e retorna o resultado completo.
     */
    @Transactional
    public ActionResultResponse processarAcao(ProcessActionRequest request) {
        Tenant tenant = SecurityUtils.getTenantAtual();
        Long tenantId = tenant.getId();

        // 1. Busca ou cria o player
        Player player = playerService.buscarOuCriar(
            tenant, request.playerId(), request.playerName());

        // 2. Valida que a ação existe e está ativa
        ActionDefinition acao = actionDefinitionRepository
            .findByTenantIdAndCodeAndActiveTrue(tenantId, request.actionCode())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Ação não encontrada ou inativa: " + request.actionCode()));

        // 3. Valida cooldown (lança CooldownAtivoException se necessário)
        cooldownService.validar(player, acao);

        // 4. Concede XP
        int xpConcedido = xpService.concederXp(player, acao);

        // 5. Verifica level up
        LevelUpDetails levelUp = levelService.verificarLevelUp(player);

        // 6. Atualiza streak
        StreakInfo streakInfo = streakService.atualizar(player);

        // 7. Avalia conquistas (pode gerar XP bônus)
        List<Achievement> conquistasDesbloqueadas = achievementEngine.avaliar(player);
        List<AchievementUnlockedResponse> novasConquistas = new ArrayList<>();

        for (Achievement conquista : conquistasDesbloqueadas) {
            novasConquistas.add(achievementEngine.toUnlockedResponse(conquista));

            // Se a conquista dá XP bônus, concede e re-verifica nível
            if (conquista.getXpReward() > 0) {
                xpService.concederXpBonus(player, conquista.getXpReward());
                LevelUpDetails noveLevelUp = levelService.verificarLevelUp(player);
                // Mantém o maior nível atingido
                if (noveLevelUp.happened()) {
                    levelUp = noveLevelUp;
                }
            }
        }

        // 8. Atualiza posição no ranking
        Integer posicaoRanking = rankingService.atualizarEObterPosicao(player);

        // 9. Dispara webhooks de forma assíncrona
        webhookService.dispararAsync(tenant, player, levelUp, conquistasDesbloqueadas, streakInfo);

        log.info("Ação '{}' processada para player '{}' (tenant {}) — XP: +{}, Nível: {}, Streak: {}",
                acao.getCode(), player.getExternalId(), tenantId,
                xpConcedido, player.getCurrentLevel(), player.getCurrentStreak());

        return new ActionResultResponse(
            player.getExternalId(),
            acao.getCode(),
            xpConcedido,
            player.getTotalXp(),
            player.getCurrentLevel(),
            levelUp,
            streakInfo,
            novasConquistas,
            posicaoRanking,
            Instant.now()
        );
    }
}
