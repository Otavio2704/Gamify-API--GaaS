package com.gamifyapi.controller;

import com.gamifyapi.dto.request.LevelConfigRequest;
import com.gamifyapi.entity.LevelConfig;
import com.gamifyapi.security.SecurityUtils;
import com.gamifyapi.service.LevelConfigService;
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
 * Configuração da tabela de níveis e XP do tenant.
 */
@RestController
@RequestMapping("/api/v1/levels")
@RequiredArgsConstructor
@Tag(name = "Níveis", description = "Configuração da tabela de progressão de níveis")
@SecurityRequirement(name = "BearerAuth")
public class LevelController {

    private final LevelConfigService levelConfigService;

    @PostMapping
    @Operation(summary = "Salva (substitui) a tabela de configuração de níveis")
    public ResponseEntity<List<LevelConfig>> salvar(
            @Valid @RequestBody LevelConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(levelConfigService.salvar(SecurityUtils.getTenantAtual(), request));
    }

    @GetMapping
    @Operation(summary = "Lista os níveis configurados do tenant")
    public ResponseEntity<List<LevelConfig>> listar() {
        return ResponseEntity.ok(levelConfigService.listar(SecurityUtils.getTenantIdAtual()));
    }
}
