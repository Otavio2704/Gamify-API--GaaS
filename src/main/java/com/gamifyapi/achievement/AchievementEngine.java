package com.gamifyapi.achievement;

import com.gamifyapi.dto.response.AchievementUnlockedResponse;
import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.Player;
import com.gamifyapi.entity.PlayerAchievement;
import com.gamifyapi.repository.AchievementRepository;
import com.gamifyapi.repository.PlayerAchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de avaliação de conquistas.
 * Avalia todas as conquistas ativas do tenant que o player ainda não desbloqueou.
 * Usa o padrão Strategy via EvaluatorFactory + AchievementCriteriaEvaluator.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AchievementEngine {

    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final EvaluatorFactory evaluatorFactory;

    /**
     * Avalia e desbloqueia conquistas para o player.
     * Não inclui concessão de XP bônus — isso é responsabilidade do GamificationService.
     *
     * @return lista de conquistas desbloqueadas nesta execução
     */
    @Transactional
    public List<Achievement> avaliar(Player player) {
        Long tenantId = player.getTenant().getId();

        List<Achievement> candidatas = achievementRepository
            .findAtivasNaoDesbloqueasPorPlayer(tenantId, player.getId());

        List<Achievement> desbloqueadas = new ArrayList<>();

        for (Achievement achievement : candidatas) {
            try {
                AchievementCriteriaEvaluator evaluator =
                    evaluatorFactory.get(achievement.getCriteriaType());

                if (evaluator.evaluate(player, achievement)) {
                    desbloquear(player, achievement);
                    desbloqueadas.add(achievement);
                    log.info("Conquista '{}' desbloqueada pelo player '{}' (tenant {})",
                            achievement.getCode(), player.getExternalId(), tenantId);
                }
            } catch (Exception e) {
                log.error("Erro ao avaliar conquista '{}' para player '{}': {}",
                        achievement.getCode(), player.getExternalId(), e.getMessage());
            }
        }

        return desbloqueadas;
    }

    private void desbloquear(Player player, Achievement achievement) {
        PlayerAchievement pa = PlayerAchievement.builder()
                .player(player)
                .achievement(achievement)
                .unlockedAt(Instant.now())
                .build();
        playerAchievementRepository.save(pa);
    }

    /**
     * Converte Achievement desbloqueada para DTO de resposta.
     */
    public AchievementUnlockedResponse toUnlockedResponse(Achievement a) {
        return new AchievementUnlockedResponse(
            a.getCode(), a.getName(), a.getDescription(),
            a.getBadgeImageUrl(), a.getXpReward(), Instant.now()
        );
    }
}
