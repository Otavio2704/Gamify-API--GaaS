package com.gamifyapi.controller;

import com.gamifyapi.dto.request.WebhookConfigRequest;
import com.gamifyapi.dto.response.WebhookConfigResponse;
import com.gamifyapi.security.SecurityUtils;
import com.gamifyapi.service.WebhookConfigService;
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
 * CRUD de configurações de webhook do tenant.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Configuração de webhooks para eventos de gamificação")
@SecurityRequirement(name = "BearerAuth")
public class WebhookController {

    private final WebhookConfigService webhookConfigService;

    @PostMapping
    @Operation(summary = "Cria uma nova configuração de webhook")
    public ResponseEntity<WebhookConfigResponse> criar(
            @Valid @RequestBody WebhookConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(webhookConfigService.criar(SecurityUtils.getTenantAtual(), request));
    }

    @GetMapping
    @Operation(summary = "Lista webhooks do tenant")
    public ResponseEntity<List<WebhookConfigResponse>> listar() {
        return ResponseEntity.ok(webhookConfigService.listar(SecurityUtils.getTenantIdAtual()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um webhook")
    public ResponseEntity<WebhookConfigResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody WebhookConfigRequest request) {
        return ResponseEntity.ok(
            webhookConfigService.atualizar(SecurityUtils.getTenantIdAtual(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um webhook")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        webhookConfigService.remover(SecurityUtils.getTenantIdAtual(), id);
        return ResponseEntity.noContent().build();
    }
}
