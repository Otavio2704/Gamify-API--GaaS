package com.gamifyapi.controller;

import com.gamifyapi.dto.request.AchievementRequest;
import com.gamifyapi.dto.response.AchievementResponse;
import com.gamifyapi.security.SecurityUtils;
import com.gamifyapi.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de conquistas (badges) do tenant.
 */
@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
@Tag(name = "Conquistas", description = "Configuração de conquistas e badges")
@SecurityRequirement(name = "BearerAuth")
public class AchievementController {

    private final AchievementService achievementService;

    @PostMapping
    @Operation(summary = "Cria uma nova conquista")
    public ResponseEntity<AchievementResponse> criar(@Valid @RequestBody AchievementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(achievementService.criar(SecurityUtils.getTenantAtual(), request));
    }

    @GetMapping
    @Operation(summary = "Lista conquistas do tenant")
    public ResponseEntity<List<AchievementResponse>> listar() {
        return ResponseEntity.ok(achievementService.listar(SecurityUtils.getTenantIdAtual()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma conquista")
    public ResponseEntity<AchievementResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AchievementRequest request) {
        return ResponseEntity.ok(
            achievementService.atualizar(SecurityUtils.getTenantIdAtual(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove (desativa) uma conquista")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        achievementService.remover(SecurityUtils.getTenantIdAtual(), id);
        return ResponseEntity.noContent().build();
    }
}
