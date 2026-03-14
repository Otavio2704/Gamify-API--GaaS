package com.gamifyapi.controller;

import com.gamifyapi.dto.response.PlayerAchievementsResponse;
import com.gamifyapi.dto.response.PlayerProfileResponse;
import com.gamifyapi.security.SecurityUtils;
import com.gamifyapi.service.PlayerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Consultas do perfil e histórico de um player.
 * Toda a lógica de negócio está em {@link PlayerProfileService}.
 */
@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "Perfil, conquistas e histórico de ações dos players")
@SecurityRequirement(name = "ApiKeyAuth")
public class PlayerController {

    private final PlayerProfileService playerProfileService;

    @GetMapping("/{externalId}")
    @Operation(summary = "Retorna o perfil completo de um player")
    public ResponseEntity<PlayerProfileResponse> obterPerfil(
            @PathVariable String externalId) {
        return ResponseEntity.ok(
                playerProfileService.obterPerfil(
                        SecurityUtils.getTenantIdAtual(), externalId));
    }

    @GetMapping("/{externalId}/achievements")
    @Operation(summary = "Lista conquistas desbloqueadas e bloqueadas do player")
    public ResponseEntity<PlayerAchievementsResponse> obterConquistas(
            @PathVariable String externalId) {
        return ResponseEntity.ok(
                playerProfileService.obterConquistas(
                        SecurityUtils.getTenantIdAtual(), externalId));
    }

    @GetMapping("/{externalId}/timeline")
    @Operation(summary = "Histórico de ações do player (paginado)")
    public ResponseEntity<List<Map<String, Object>>> obterTimeline(
            @PathVariable String externalId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                playerProfileService.obterTimeline(
                        SecurityUtils.getTenantIdAtual(), externalId, page, size));
    }
}
