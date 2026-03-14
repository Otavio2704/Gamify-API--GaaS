package com.gamifyapi.achievement;

import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.Player;
import com.gamifyapi.repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Avalia critério MULTI_ACTION:
 * Verifica se o player executou cada uma das ações da lista ao menos uma vez.
 * criteriaValue: {"actionCodes": ["completed_lesson", "passed_quiz", "submitted_project"]}
 */
@Component
@RequiredArgsConstructor
public class MultiActionEvaluator implements AchievementCriteriaEvaluator {

    private final ActionLogRepository actionLogRepository;

    @Override
    public boolean evaluate(Player player, Achievement achievement) {
        List<String> actionCodes = achievement.getCriteriaValueAsList("actionCodes");

        long codigosConcluidos = actionLogRepository
            .countDistinctActionCodesForPlayer(player.getId(), actionCodes);

        return codigosConcluidos >= actionCodes.size();
    }
}
