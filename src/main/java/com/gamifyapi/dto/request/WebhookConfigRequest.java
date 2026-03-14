package com.gamifyapi.dto.request;

import com.gamifyapi.enums.WebhookEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar ou atualizar uma configuração de webhook.
 */
public record WebhookConfigRequest(
    @NotBlank(message = "URL é obrigatória")
    @Pattern(regexp = "https://.*", message = "URL do webhook deve começar com https://")
    @Size(max = 500)
    String url,

    @NotNull(message = "eventType é obrigatório")
    WebhookEventType eventType,

    @NotBlank(message = "secretKey é obrigatório")
    String secretKey
) {}
