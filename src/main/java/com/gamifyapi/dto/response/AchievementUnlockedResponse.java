package com.gamifyapi.dto.response;

import java.time.Instant;

/**
 * Conquista recém-desbloqueada pelo player durante o processamento de uma ação.
 */
public record AchievementUnlockedResponse(
    String code,
    String name,
    String description,
    String badgeImageUrl,
    Integer xpBonus,
    Instant unlockedAt
) {}
