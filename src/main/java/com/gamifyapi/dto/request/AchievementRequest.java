package com.gamifyapi.dto.request;

import com.gamifyapi.enums.CriteriaType;
import jakarta.validation.constraints.*;

import java.util.Map;

/**
 * Dados para criar ou atualizar uma conquista (achievement).
 */
public record AchievementRequest(
    @NotBlank(message = "Código é obrigatório")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Código deve ser UPPER_SNAKE_CASE")
    String code,

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100)
    String name,

    @Size(max = 255)
    String description,

    @Size(max = 500)
    String badgeImageUrl,

    @Min(0)
    Integer xpReward,

    @NotNull(message = "criteriaType é obrigatório")
    CriteriaType criteriaType,

    @NotNull(message = "criteriaValue é obrigatório")
    Map<String, Object> criteriaValue
) {
    public AchievementRequest {
        if (xpReward == null) xpReward = 0;
    }
}
