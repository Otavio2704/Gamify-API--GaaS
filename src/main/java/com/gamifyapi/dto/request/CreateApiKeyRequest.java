package com.gamifyapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados para criação de uma nova API Key.
 */
public record CreateApiKeyRequest(
    @NotBlank(message = "Label é obrigatório")
    @Size(max = 50, message = "Label deve ter no máximo 50 caracteres")
    String label
) {}
