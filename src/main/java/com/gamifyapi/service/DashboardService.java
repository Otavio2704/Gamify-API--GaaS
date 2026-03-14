package com.gamifyapi.service;

import com.gamifyapi.dto.response.DashboardOverviewResponse;
import com.gamifyapi.dto.response.DashboardOverviewResponse.TopAction;
import com.gamifyapi.repository.ActionLogRepository;
import com.gamifyapi.repository.PlayerAchievementRepository;
import com.gamifyapi.repository.PlayerRepository;
import com.gamifyapi.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de métricas e visão geral do dashboard do tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final PlayerRepository playerRepository;
    private final ActionLogRepository actionLogRepository;
    private final PlayerAchievementRepository playerAchievementRepository;

    /**
     * Retorna visão geral agregada do tenant.
     */
    @Transactional(readOnly = true)
    public DashboardOverviewResponse obterOverview() {
        Long tenantId = SecurityUtils.getTenantIdAtual();
        Instant seteDiasAtras = Instant.now().minus(7, ChronoUnit.DAYS);
        LocalDate seteDiasAtrasDate = LocalDate.now().minusDays(7);

        long totalPlayers = playerRepository.countByTenant_Id(tenantId);
        long playersAtivos7d = playerRepository
            .countByTenant_IdAndLastActivityDateAfter(tenantId, seteDiasAtrasDate);
        long totalAcoes = actionLogRepository.countByTenantId(tenantId);
        long acoes7d = actionLogRepository.countByTenantIdSince(tenantId, seteDiasAtras);
        Double nivelMedioRaw = playerRepository.findAverageLevelByTenantId(tenantId);
        double nivelMedio = nivelMedioRaw != null ? nivelMedioRaw : 1.0;
        long conquistasDesbloqueadas = playerAchievementRepository.countByTenantId(tenantId);

        List<Object[]> topList = actionLogRepository
            .findTopActionByTenantId(tenantId, PageRequest.of(0, 1));
        TopAction topAcao = topList.isEmpty() ? null
            : new TopAction((String) topList.get(0)[0], ((Number) topList.get(0)[1]).longValue());

        return new DashboardOverviewResponse(
            totalPlayers,
            playersAtivos7d,
            totalAcoes,
            acoes7d,
            nivelMedio,
            conquistasDesbloqueadas,
            topAcao
        );
    }

    /**
     * Retorna contagem de ações agrupadas por dia.
     *
     * @param dias número de dias a considerar
     * @return lista de mapas com chaves "date" e "count"
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obterGraficoAcoes(int dias) {
        Long tenantId = SecurityUtils.getTenantIdAtual();
        Instant desde = Instant.now().minus(dias, ChronoUnit.DAYS);
        List<Object[]> rows = actionLogRepository.countByTenantIdGroupByDay(tenantId, desde);
        return rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("date", r[0] != null ? r[0].toString() : null);
            m.put("count", ((Number) r[1]).longValue());
            return m;
        }).toList();
    }
}
