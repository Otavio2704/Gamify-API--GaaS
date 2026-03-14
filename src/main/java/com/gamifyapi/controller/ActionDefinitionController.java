package com.gamifyapi.controller;

import com.gamifyapi.dto.request.ActionDefinitionRequest;
import com.gamifyapi.dto.response.ActionDefinitionResponse;
import com.gamifyapi.security.SecurityUtils;
import com.gamifyapi.service.ActionDefinitionService;
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
 * CRUD de definições de ações de gamificação do tenant.
 */
@RestController
@RequestMapping("/api/v1/actions/definitions")
@RequiredArgsConstructor
@Tag(name = "Definições de Ações", description = "Configuração das ações disponíveis para gamificação")
@SecurityRequirement(name = "BearerAuth")
public class ActionDefinitionController {

    private final ActionDefinitionService actionDefinitionService;

    @PostMapping
    @Operation(summary = "Cria uma nova definição de ação")
    public ResponseEntity<ActionDefinitionResponse> criar(
            @Valid @RequestBody ActionDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(actionDefinitionService.criar(SecurityUtils.getTenantAtual(), request));
    }

    @GetMapping
    @Operation(summary = "Lista definições de ações do tenant")
    public ResponseEntity<List<ActionDefinitionResponse>> listar() {
        return ResponseEntity.ok(actionDefinitionService.listar(SecurityUtils.getTenantIdAtual()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma definição de ação")
    public ResponseEntity<ActionDefinitionResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActionDefinitionRequest request) {
        return ResponseEntity.ok(
            actionDefinitionService.atualizar(SecurityUtils.getTenantIdAtual(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove (desativa) uma definição de ação")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        actionDefinitionService.remover(SecurityUtils.getTenantIdAtual(), id);
        return ResponseEntity.noContent().build();
    }
}
