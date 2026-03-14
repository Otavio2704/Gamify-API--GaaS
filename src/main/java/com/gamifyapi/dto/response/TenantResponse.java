package com.gamifyapi.dto.response;

import com.gamifyapi.enums.TenantPlan;

import java.time.LocalDateTime;

/**
 * Resposta de cadastro/dados do tenant.
 */
public record TenantResponse(
    Long id,
    String name,
    String email,
    TenantPlan plan,
    LocalDateTime createdAt
) {}
