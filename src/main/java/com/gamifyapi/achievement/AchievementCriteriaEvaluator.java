package com.gamifyapi.achievement;

import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.Player;

/**
 * Contrato do padrão Strategy para avaliação de critérios de conquistas.
 * Cada implementação avalia um tipo específico de CriteriaType.
 */
public interface AchievementCriteriaEvaluator {

    /**
     * Avalia se o player cumpriu o critério da conquista.
     *
     * @param player      player sendo avaliado
     * @param achievement conquista com os critérios a verificar
     * @return true se o critério foi atingido
     */
    boolean evaluate(Player player, Achievement achievement);
}
