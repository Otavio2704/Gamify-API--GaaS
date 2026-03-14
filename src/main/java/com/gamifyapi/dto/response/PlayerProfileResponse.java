package com.gamifyapi.dto.response;

/**
 * Perfil completo de um player.
 */
public record PlayerProfileResponse(
    String externalId,
    String displayName,
    Integer totalXp,
    Integer currentLevel,
    String levelTitle,
    Integer xpToNextLevel,
    Double progressPercent,
    Integer currentStreak,
    Integer longestStreak,
    Long totalActions,
    Long achievementsUnlocked,
    Integer leaderboardPosition,
    java.time.LocalDateTime memberSince
) {}
