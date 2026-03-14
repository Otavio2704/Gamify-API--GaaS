package com.gamifyapi.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Dados para processar uma ação de um player (endpoint principal via API Key).
 */
public record ProcessActionRequest(
    @NotBlank(message = "playerId é obrigatório")
    String playerId,

    /** Nome de exibição do player (opcional — atualizado se diferente do atual). */
    String playerName,

    @NotBlank(message = "actionCode é obrigatório")
    String actionCode
) {}
