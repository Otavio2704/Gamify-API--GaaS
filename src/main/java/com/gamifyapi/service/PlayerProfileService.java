package com.gamifyapi.service;

import com.gamifyapi.dto.response.PlayerAchievementsResponse;
import com.gamifyapi.dto.response.PlayerProfileResponse;
import com.gamifyapi.entity.Player;
import com.gamifyapi.exception.RecursoNaoEncontradoException;
import com.gamifyapi.repository.ActionLogRepository;
import com.gamifyapi.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Centraliza as consultas de perfil, conquistas e timeline de um player.
 * Mantém o controller enxuto e a lógica de negócio testável de forma isolada.
 */
@Service
@RequiredArgsConstructor
public class PlayerProfileService {

    private final PlayerRepository    playerRepository;
    private final ActionLogRepository actionLogRepository;
    private final AchievementService  achievementService;
    private final LevelService        levelService;
    private final RankingService      rankingService;

    @Transactional(readOnly = true)
    public PlayerProfileResponse obterPerfil(Long tenantId, String externalId) {
        Player  player         = buscarPlayer(tenantId, externalId);
        String  levelTitle     = levelService.getTitulo(tenantId, player.getCurrentLevel());
        Integer xpToNextLevel  = levelService.getXpProximoNivel(
                tenantId, player.getCurrentLevel(), player.getTotalXp());
        long    totalActions   = actionLogRepository.countByPlayerId(player.getId());
        double  progressPercent = calcularProgresso(player.getTotalXp(), xpToNextLevel);
        long    achievementsUnlocked = achievementService
                .obterConquistasDoPlayer(tenantId, player.getId())
                .unlocked().size();
        Integer leaderboardPosition = rankingService.obterPosicaoGlobalDoPlayer(player.getId());

        return new PlayerProfileResponse(
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
        );
    }

    @Transactional(readOnly = true)
    public PlayerAchievementsResponse obterConquistas(Long tenantId, String externalId) {
        Player player = buscarPlayer(tenantId, externalId);
        return achievementService.obterConquistasDoPlayer(tenantId, player.getId());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obterTimeline(Long tenantId, String externalId,
                                                    int page, int size) {
        Player player = buscarPlayer(tenantId, externalId);
        return actionLogRepository
                .findAllByPlayerIdOrderByTimestampDesc(
                        player.getId(), PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(al -> Map.<String, Object>of(
                        "actionCode", al.getActionDefinition().getCode(),
                        "xpGranted",  al.getXpGranted(),
                        "timestamp",  al.getTimestamp().toString()
                ))
                .toList();
    }

    // -------------------------------------------------------------------------

    private Player buscarPlayer(Long tenantId, String externalId) {
        return playerRepository.findByTenant_IdAndExternalId(tenantId, externalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Player não encontrado: " + externalId));
    }

    private double calcularProgresso(int totalXp, Integer xpToNextLevel) {
        if (xpToNextLevel == null || xpToNextLevel <= 0) return 100.0;
        return Math.min(100.0,
                (1.0 - (double) xpToNextLevel / (totalXp + xpToNextLevel)) * 100);
    }
}
