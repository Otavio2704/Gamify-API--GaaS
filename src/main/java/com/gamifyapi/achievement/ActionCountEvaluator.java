package com.gamifyapi.achievement;

import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.Player;
import com.gamifyapi.repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Avalia critério ACTION_COUNT:
 * Verifica se o player executou uma ação específica N ou mais vezes.
 * criteriaValue: {"actionCode": "completed_lesson", "count": 50}
 */
@Component
@RequiredArgsConstructor
public class ActionCountEvaluator implements AchievementCriteriaEvaluator {

    private final ActionLogRepository actionLogRepository;

    @Override
    public boolean evaluate(Player player, Achievement achievement) {
        String actionCode = (String) achievement.getCriteriaValueAsMap().get("actionCode");
        int countNecessario = achievement.getCriteriaValueAsInt("count");

        long contagem = actionLogRepository
            .countByPlayerIdAndActionCode(player.getId(), actionCode);

        return contagem >= countNecessario;
    }
}
