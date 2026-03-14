package com.gamifyapi.dto.request;

import jakarta.validation.constraints.*;

/**
 * Dados para criar ou atualizar uma definição de ação.
 */
public record ActionDefinitionRequest(
    @NotBlank(message = "Código é obrigatório")
    @Size(min = 3, max = 50, message = "Código deve ter entre 3 e 50 caracteres")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "Código deve ser snake_case (letras minúsculas, números e underscore)")
    String code,

    @NotBlank(message = "Nome de exibição é obrigatório")
    @Size(max = 100)
    String displayName,

    @Size(max = 255)
    String description,

    @NotNull(message = "xpValue é obrigatório")
    @Positive(message = "xpValue deve ser maior que zero")
    Integer xpValue,

    @Min(value = 0, message = "cooldownSeconds deve ser maior ou igual a zero")
    Integer cooldownSeconds
) {
    public ActionDefinitionRequest {
        if (cooldownSeconds == null) cooldownSeconds = 0;
    }
}
