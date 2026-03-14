package com.gamifyapi.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Conquistas desbloqueadas e bloqueadas de um player.
 */
public record PlayerAchievementsResponse(
    List<UnlockedAchievement> unlocked,
    List<LockedAchievement> locked
) {

    public record UnlockedAchievement(
        String code,
        String name,
        String description,
        String badgeImageUrl,
        Instant unlockedAt
    ) {}

    public record LockedAchievement(
        String code,
        String name,
        String description,
        String badgeImageUrl,
        String progressDescription
    ) {}
}
