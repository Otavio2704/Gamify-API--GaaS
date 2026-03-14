package com.gamifyapi.controller;

import com.gamifyapi.dto.response.PlayerAchievementsResponse;
import com.gamifyapi.dto.response.PlayerProfileResponse;
import com.gamifyapi.entity.Player;
import com.gamifyapi.exception.RecursoNaoEncontradoException;
import com.gamifyapi.repository.ActionLogRepository;
import com.gamifyapi.repository.PlayerRepository;
import com.gamifyapi.security.SecurityUtils;
import com.gamifyapi.service.AchievementService;
import com.gamifyapi.service.LevelService;
import com.gamifyapi.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Consultas do perfil e histórico de um player.
 * Autenticado via API Key (integração) ou JWT (admin).
 */
@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "Perfil, conquistas e histórico de ações dos players")
@SecurityRequirement(name = "ApiKeyAuth")
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final ActionLogRepository actionLogRepository;
    private final AchievementService achievementService;
    private final LevelService levelService;
    private final RankingService rankingService;

    @GetMapping("/{externalId}")
    @Operation(summary = "Retorna o perfil completo de um player")
    public ResponseEntity<PlayerProfileResponse> obterPerfil(@PathVariable String externalId) {
        Long tenantId = SecurityUtils.getTenantIdAtual();
        Player player = buscarPlayer(tenantId, externalId);

        String levelTitle = levelService.getTitulo(tenantId, player.getCurrentLevel());
        Integer xpToNextLevel = levelService.getXpProximoNivel(
            tenantId, player.getCurrentLevel(), player.getTotalXp());
        long totalActions = actionLogRepository.countByPlayerId(player.getId());

        double progressPercent = calcularProgresso(player.getTotalXp(), xpToNextLevel);
        long achievementsUnlocked = achievementService
            .obterConquistasDoPlayer(tenantId, player.getId())
            .unlocked().size();
        Integer leaderboardPosition = rankingService.obterPosicaoGlobalDoPlayer(player.getId());

        return ResponseEntity.ok(new PlayerProfileResponse(
            player.getExternalId(),
            player.getDisplayName(),
            player.getTotalXp(),
            player.getCurrentLevel(),
            levelTitle,
            xpToNextLevel,
            progressPercent,
            player.getCurrentStreak(),
            player.getLongestStreak(),
            totalActions,
            achievementsUnlocked,
            leaderboardPosition,
            player.getCreatedAt()
        ));
    }

    @GetMapping("/{externalId}/achievements")
    @Operation(summary = "Lista conquistas desbloqueadas e bloqueadas do player")
    public ResponseEntity<PlayerAchievementsResponse> obterConquistas(
            @PathVariable String externalId) {
        Long tenantId = SecurityUtils.getTenantIdAtual();
        Player player = buscarPlayer(tenantId, externalId);
        return ResponseEntity.ok(
            achievementService.obterConquistasDoPlayer(tenantId, player.getId()));
    }

    @GetMapping("/{externalId}/timeline")
    @Operation(summary = "Histórico de ações do player (paginado)")
    public ResponseEntity<List<Map<String, Object>>> obterTimeline(
            @PathVariable String externalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long tenantId = SecurityUtils.getTenantIdAtual();
        Player player = buscarPlayer(tenantId, externalId);

        return ResponseEntity.ok(
            actionLogRepository
                .findAllByPlayerIdOrderByTimestampDesc(
                    player.getId(), PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(al -> Map.<String, Object>of(
                    "actionCode", al.getActionDefinition().getCode(),
                    "xpGranted", al.getXpGranted(),
                    "timestamp", al.getTimestamp().toString()
                ))
                .toList()
        );
    }

    // -------------------------------------------------------------------------

    private Player buscarPlayer(Long tenantId, String externalId) {
        return playerRepository.findByTenant_IdAndExternalId(tenantId, externalId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Player não encontrado: " + externalId));
    }

    private double calcularProgresso(int totalXp, Integer xpToNextLevel) {
        if (xpToNextLevel == null || xpToNextLevel <= 0) return 100.0;
        // progresso relativo ao nível atual
        return Math.min(100.0, (1.0 - (double) xpToNextLevel / (totalXp + xpToNextLevel)) * 100);
    }
}
