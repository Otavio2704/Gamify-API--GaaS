package com.gamifyapi.dto.response;

import java.time.LocalDateTime;

public record ActionDefinitionResponse(
    Long id,
    String code,
    String displayName,
    String description,
    Integer xpValue,
    Integer cooldownSeconds,
    Boolean active,
    LocalDateTime createdAt
) {}
