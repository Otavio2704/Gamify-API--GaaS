package com.gamifyapi.dto.response;

/**
 * Visão geral do dashboard do tenant.
 */
public record DashboardOverviewResponse(
    Long totalPlayers,
    Long activePlayers7d,
    Long totalActionsAllTime,
    Long totalActions7d,
    Double averageLevel,
    Long achievementsUnlockedTotal,
    TopAction topAction
) {
    public record TopAction(String code, Long count) {}
}
