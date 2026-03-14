package com.gamifyapi.controller;

import com.gamifyapi.dto.response.LeaderboardResponse;
import com.gamifyapi.enums.RankingPeriod;
import com.gamifyapi.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Consulta de leaderboard por período.
 */
@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Rankings de players por período")
@SecurityRequirement(name = "ApiKeyAuth")
public class LeaderboardController {

    private final RankingService rankingService;

    @GetMapping
    @Operation(summary = "Leaderboard global (por XP total)")
    public ResponseEntity<LeaderboardResponse> obterGlobal(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(rankingService.obterLeaderboard(RankingPeriod.GLOBAL, page, size));
    }

    @GetMapping("/weekly")
    @Operation(summary = "Leaderboard da semana atual")
    public ResponseEntity<LeaderboardResponse> obterSemanal(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(rankingService.obterLeaderboard(RankingPeriod.WEEKLY, page, size));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Leaderboard do mês atual")
    public ResponseEntity<LeaderboardResponse> obterMensal(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(rankingService.obterLeaderboard(RankingPeriod.MONTHLY, page, size));
    }
}
