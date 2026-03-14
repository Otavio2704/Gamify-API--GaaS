package com.gamifyapi.controller;

import com.gamifyapi.dto.response.DashboardOverviewResponse;
import com.gamifyapi.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dashboard de métricas do tenant.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Métricas e visão geral do tenant")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Visão geral com métricas agregadas do tenant")
    public ResponseEntity<DashboardOverviewResponse> obterOverview() {
        return ResponseEntity.ok(dashboardService.obterOverview());
    }

    @GetMapping("/actions-chart")
    @Operation(summary = "Gráfico de ações por dia nos últimos N dias")
    public ResponseEntity<List<Map<String, Object>>> obterGraficoAcoes(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(dashboardService.obterGraficoAcoes(dias));
    }
}
