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
import java.util.ArrayList;
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
    private final LevelConfigRepository levelConfigRepository;

    /**
     * Atualiza a pontuação do player no ranking GLOBAL e retorna sua posição.
     */
    @Transactional
    public Integer atualizarEObterPosicao(Player player) {
        Long tenantId = player.getTenant().getId();
        atualizarEntradaComRecalculoPosicoes(player, RankingPeriod.GLOBAL, "GLOBAL", player.getTotalXp());
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

        List<LeaderboardEntry> itens = new ArrayList<>();
        for (RankingEntry re : page.getContent()) {
            String titulo = obterTituloNivel(tenantId, re.getPlayer().getCurrentLevel());
            itens.add(new LeaderboardEntry(
                re.getPosition(),
                re.getPlayer().getExternalId(),
                re.getPlayer().getDisplayName(),
                re.getScore(),
                re.getPlayer().getCurrentLevel(),
                titulo
            ));
        }

        long total = rankingEntryRepository.countByTenantIdAndPeriodAndPeriodKey(
            tenantId, period, periodKey);

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
    // Métodos privados
    // -------------------------------------------------------------------------

    private void atualizarEntradaComRecalculoPosicoes(Player player, RankingPeriod period,
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

        // Recalcula posições de todos no período (simples: ordenado por score desc)
        recalcularPosicoes(tenantId, period, periodKey);
    }

    private void recalcularPosicoes(Long tenantId, RankingPeriod period, String periodKey) {
        Page<RankingEntry> todasEntradas = rankingEntryRepository
            .findAllByTenantIdAndPeriodAndPeriodKeyOrderByPositionAsc(
                tenantId, period, periodKey, PageRequest.of(0, Integer.MAX_VALUE));

        // Ordena por score descrescente para recalcular posições
        List<RankingEntry> ordenadas = todasEntradas.getContent().stream()
            .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
            .toList();

        for (int i = 0; i < ordenadas.size(); i++) {
            RankingEntry re = ordenadas.get(i);
            re.setPosition(i + 1);
            re.setUpdatedAt(Instant.now());
        }
        rankingEntryRepository.saveAll(ordenadas);
    }

    private Integer obterPosicao(Long tenantId, Long playerId, RankingPeriod period, String periodKey) {
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
                LocalDate inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                int semana = inicioSemana.get(WeekFields.ISO.weekOfWeekBasedYear());
                yield inicioSemana.getYear() + "-W" + String.format("%02d", semana);
            }
            case MONTHLY -> hoje.getYear() + "-" + String.format("%02d", hoje.getMonthValue());
        };
    }
}
