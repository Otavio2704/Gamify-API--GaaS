package com.gamifyapi.controller;

import com.gamifyapi.dto.request.ProcessActionRequest;
import com.gamifyapi.dto.response.ActionResultResponse;
import com.gamifyapi.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint principal de processamento de ação de um player.
 * Autenticado via API Key ({@code X-API-Key}).
 */
@RestController
@RequestMapping("/api/v1/actions")
@RequiredArgsConstructor
@Tag(name = "Ações", description = "Endpoint de integração para processar ações dos players")
@SecurityRequirement(name = "ApiKeyAuth")
public class ActionController {

    private final GamificationService gamificationService;

    @PostMapping
    @Operation(
        summary = "Processa uma ação de um player",
        description = "Orquestra XP, level up, streak, conquistas e ranking. " +
                      "Autenticado via header X-API-Key."
    )
    public ResponseEntity<ActionResultResponse> processarAcao(
            @Valid @RequestBody ProcessActionRequest request) {
        return ResponseEntity.ok(gamificationService.processarAcao(request));
    }
}
