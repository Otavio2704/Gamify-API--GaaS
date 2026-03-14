package com.gamifyapi.achievement;

import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.Player;
import org.springframework.stereotype.Component;

/**
 * Avalia critério LEVEL_REACHED:
 * Verifica se o player atingiu o nível mínimo configurado.
 * criteriaValue: {"level": 10}
 */
@Component
public class LevelReachedEvaluator implements AchievementCriteriaEvaluator {

    @Override
    public boolean evaluate(Player player, Achievement achievement) {
        int nivelNecessario = achievement.getCriteriaValueAsInt("level");
        return player.getCurrentLevel() >= nivelNecessario;
    }
}
