package com.gamifyapi.achievement;

import com.gamifyapi.enums.CriteriaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory que seleciona o evaluator correto com base no CriteriaType.
 * Implementa o padrão Factory para desacoplar o engine dos evaluators específicos.
 */
@Component
@RequiredArgsConstructor
public class EvaluatorFactory {

    private final ActionCountEvaluator actionCountEvaluator;
    private final StreakEvaluator streakEvaluator;
    private final LevelReachedEvaluator levelReachedEvaluator;
    private final XpTotalEvaluator xpTotalEvaluator;
    private final MultiActionEvaluator multiActionEvaluator;

    /**
     * Retorna o evaluator correspondente ao tipo de critério.
     *
     * @throws IllegalArgumentException se o tipo não for suportado
     */
    public AchievementCriteriaEvaluator get(CriteriaType tipo) {
        return switch (tipo) {
            case ACTION_COUNT -> actionCountEvaluator;
            case STREAK -> streakEvaluator;
            case LEVEL_REACHED -> levelReachedEvaluator;
            case XP_TOTAL -> xpTotalEvaluator;
            case MULTI_ACTION -> multiActionEvaluator;
        };
    }
}
