package com.gamifyapi.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Resposta do endpoint principal — processamento de ação de um player.
 */
public record ActionResultResponse(
    String playerId,
    String action,
    Integer xpGranted,
    Integer totalXp,
    Integer currentLevel,
    LevelUpDetails levelUp,
    StreakInfo streak,
    List<AchievementUnlockedResponse> newAchievements,
    Integer leaderboardPosition,
    Instant processedAt
) {

    /**
     * Detalhes de level up (ou ausência dele).
     */
    public record LevelUpDetails(
        boolean happened,
        Integer previousLevel,
        Integer newLevel,
        String title
    ) {
        /** Cria um resultado indicando que não houve level up. */
        public static LevelUpDetails nenhum(int nivelAtual) {
            return new LevelUpDetails(false, nivelAtual, nivelAtual, null);
        }

        /** Cria um resultado indicando level up de {@code de} para {@code para}. */
        public static LevelUpDetails de(int de, int para, String titulo) {
            return new LevelUpDetails(true, de, para, titulo);
        }
    }

    /**
     * Informações de streak após o processamento da ação.
     */
    public record StreakInfo(
        Integer currentStreak,
        Integer longestStreak,
        boolean wasReset
    ) {}
}
