package com.gamifyapi.achievement;

import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.Player;
import org.springframework.stereotype.Component;

/**
 * Avalia critério XP_TOTAL:
 * Verifica se o player acumulou XP total mínimo.
 * criteriaValue: {"minXp": 5000}
 */
@Component
public class XpTotalEvaluator implements AchievementCriteriaEvaluator {

    @Override
    public boolean evaluate(Player player, Achievement achievement) {
        int minXp = achievement.getCriteriaValueAsInt("minXp");
        return player.getTotalXp() >= minXp;
    }
}
