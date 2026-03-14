package com.gamifyapi.dto.response;

import com.gamifyapi.enums.RankingPeriod;

import java.util.List;

/**
 * Leaderboard paginado.
 */
public record LeaderboardResponse(
    RankingPeriod period,
    List<LeaderboardEntry> entries,
    int page,
    int size,
    long totalPlayers
) {

    public record LeaderboardEntry(
        Integer position,
        String externalId,
        String displayName,
        Integer totalXp,
        Integer level,
        String levelTitle
    ) {}
}
