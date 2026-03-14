package com.gamifyapi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Dados para configurar a progressão de níveis do tenant.
 * O POST substitui toda a tabela de níveis.
 */
public record LevelConfigRequest(
    @NotEmpty(message = "A lista de níveis não pode ser vazia")
    @Valid
    List<LevelEntry> levels
) {
    public record LevelEntry(
        @NotNull @Positive Integer level,
        @NotNull @Min(0) Integer xpRequired,
        @Size(max = 50) String title
    ) {}
}
