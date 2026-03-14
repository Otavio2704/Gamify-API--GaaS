package com.gamifyapi.service;

import com.gamifyapi.dto.response.LeaderboardResponse;
import com.gamifyapi.dto.response.LeaderboardResponse.LeaderboardEntry;
import com.gamifyapi.entity.LevelConfig;
import com.gamifyapi.entity.Player;
import com.gamifyapi.entity.RankingEntry;
import com.gamifyapi.enums.RankingPeriod;
import com.gamifyapi.repository.LevelConfigRepository;
import com.gamifyapi.repository.RankingEntryRepository;
import com.gamifyapi.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Optional;

/**
 * Gerencia cálculo e consulta de rankings (GLOBAL, WEEKLY, MONTHLY).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final RankingEntryRepository rankingEntryRepository;
    private final LevelConfigRepository  levelConfigRepository;

    /**
     * Atualiza a pontuação do player no ranking GLOBAL e retorna sua posição.
     */
    @Transactional
    public Integer atualizarEObterPosicao(Player player) {
        Long tenantId = player.getTenant().getId();
        atualizarEntrada(player, RankingPeriod.GLOBAL, "GLOBAL", player.getTotalXp());
        return obterPosicao(tenantId, player.getId(), RankingPeriod.GLOBAL, "GLOBAL");
    }

    /**
     * Retorna o leaderboard para um período específico.
     */
    @Transactional(readOnly = true)
    public LeaderboardResponse obterLeaderboard(RankingPeriod period, int pagina, int tamanho) {
        Long tenantId = SecurityUtils.getTenantIdAtual();
        String periodKey = gerarPeriodKey(period);

        Page<RankingEntry> page = rankingEntryRepository
                .findAllByTenantIdAndPeriodAndPeriodKeyOrderByPositionAsc(
                        tenantId, period, periodKey, PageRequest.of(pagina, tamanho));

        List<LeaderboardEntry> itens = page.getContent().stream()
                .map(re -> new LeaderboardEntry(
                        re.getPosition(),
                        re.getPlayer().getExternalId(),
                        re.getPlayer().getDisplayName(),
                        re.getScore(),
                        re.getPlayer().getCurrentLevel(),
                        obterTituloNivel(tenantId, re.getPlayer().getCurrentLevel())
                ))
                .toList();

        long total = rankingEntryRepository
                .countByTenantIdAndPeriodAndPeriodKey(tenantId, period, periodKey);

        return new LeaderboardResponse(period, itens, pagina, tamanho, total);
    }

    /**
     * Retorna a posição global de um player no ranking do tenant atual.
     */
    @Transactional(readOnly = true)
    public Integer obterPosicaoGlobalDoPlayer(Long playerId) {
        Long tenantId = SecurityUtils.getTenantIdAtual();
        return rankingEntryRepository
                .findByTenantPlayerAndPeriod(tenantId, playerId, RankingPeriod.GLOBAL, "GLOBAL")
                .map(RankingEntry::getPosition)
                .orElse(null);
    }

    // -------------------------------------------------------------------------

    private void atualizarEntrada(Player player, RankingPeriod period,
                                   String periodKey, int novoScore) {
        Long tenantId = player.getTenant().getId();

        Optional<RankingEntry> existente = rankingEntryRepository
                .findByTenantPlayerAndPeriod(tenantId, player.getId(), period, periodKey);

        RankingEntry entry = existente.orElseGet(() -> {
            RankingEntry nova = new RankingEntry();
            nova.setTenant(player.getTenant());
            nova.setPlayer(player);
            nova.setPeriod(period);
            nova.setPeriodKey(periodKey);
            nova.setPosition(1);
            return nova;
        });

        entry.setScore(novoScore);
        entry.setUpdatedAt(Instant.now());
        rankingEntryRepository.save(entry);

        // Recalcula posições diretamente no banco — sem carregar registros em memória
        rankingEntryRepository.recalcularPosicoes(
                tenantId, period.name(), periodKey, Instant.now());
    }

    private Integer obterPosicao(Long tenantId, Long playerId,
                                  RankingPeriod period, String periodKey) {
        return rankingEntryRepository
                .findByTenantPlayerAndPeriod(tenantId, playerId, period, periodKey)
                .map(RankingEntry::getPosition)
                .orElse(null);
    }

    private String obterTituloNivel(Long tenantId, int nivel) {
        return levelConfigRepository.findAllByTenantIdOrderByLevelAsc(tenantId).stream()
                .filter(lc -> lc.getLevel().equals(nivel))
                .map(LevelConfig::getTitle)
                .findFirst()
                .orElse(null);
    }

    public static String gerarPeriodKey(RankingPeriod period) {
        LocalDate hoje = LocalDate.now(ZoneOffset.UTC);
        return switch (period) {
            case GLOBAL -> "GLOBAL";
            case WEEKLY -> {
                LocalDate inicio = hoje.with(
                        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                int semana = inicio.get(WeekFields.ISO.weekOfWeekBasedYear());
                yield inicio.getYear() + "-W" + String.format("%02d", semana);
            }
            case MONTHLY ->
                    hoje.getYear() + "-" + String.format("%02d", hoje.getMonthValue());
        };
    }
}
