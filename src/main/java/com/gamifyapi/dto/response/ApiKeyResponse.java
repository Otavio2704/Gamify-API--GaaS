package com.gamifyapi.dto.response;

import java.time.LocalDateTime;

/**
 * Resposta de API Key.
 * O campo {@code key} é populado APENAS na criação. Depois, só o prefix é visível.
 */
public record ApiKeyResponse(
    Long id,
    String key,
    String prefix,
    String label,
    Boolean active,
    LocalDateTime createdAt
) {}
