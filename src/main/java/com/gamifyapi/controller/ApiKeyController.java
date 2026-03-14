package com.gamifyapi.controller;

import com.gamifyapi.dto.request.CreateApiKeyRequest;
import com.gamifyapi.dto.response.ApiKeyResponse;
import com.gamifyapi.service.ApiKeyService;
import com.gamifyapi.security.SecurityUtils;
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
 * Gerenciamento de API Keys do tenant autenticado via JWT.
 */
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "CRUD de API Keys do tenant")
@SecurityRequirement(name = "BearerAuth")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @Operation(summary = "Cria uma nova API Key")
    public ResponseEntity<ApiKeyResponse> criar(@Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(apiKeyService.criar(SecurityUtils.getTenantAtual(), request));
    }

    @GetMapping
    @Operation(summary = "Lista as API Keys do tenant")
    public ResponseEntity<List<ApiKeyResponse>> listar() {
        return ResponseEntity.ok(apiKeyService.listarPorTenant(SecurityUtils.getTenantIdAtual()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma API Key")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        apiKeyService.remover(SecurityUtils.getTenantIdAtual(), id);
        return ResponseEntity.noContent().build();
    }
}
