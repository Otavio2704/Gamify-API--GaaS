package com.gamifyapi.achievement;

import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.Player;
import org.springframework.stereotype.Component;

/**
 * Avalia critério STREAK:
 * Verifica se o player possui streak atual >= ao mínimo configurado.
 * criteriaValue: {"minStreak": 7}
 */
@Component
public class StreakEvaluator implements AchievementCriteriaEvaluator {

    @Override
    public boolean evaluate(Player player, Achievement achievement) {
        int minStreak = achievement.getCriteriaValueAsInt("minStreak");
        return player.getCurrentStreak() >= minStreak;
    }
}
