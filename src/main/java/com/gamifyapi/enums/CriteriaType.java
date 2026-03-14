package com.gamifyapi.enums;

/**
 * Tipos de critério para avaliação de conquistas.
 */
public enum CriteriaType {
    /** Quantidade de vezes que uma ação específica foi executada. */
    ACTION_COUNT,
    /** Streak mínimo de dias consecutivos. */
    STREAK,
    /** Nível mínimo atingido pelo player. */
    LEVEL_REACHED,
    /** XP total mínimo acumulado. */
    XP_TOTAL,
    /** Execução de múltiplas ações distintas ao menos uma vez. */
    MULTI_ACTION
}
