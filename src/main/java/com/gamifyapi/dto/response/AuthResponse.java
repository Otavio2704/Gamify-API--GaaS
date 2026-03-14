package com.gamifyapi.dto.response;

/**
 * Resposta de login com token JWT.
 */
public record AuthResponse(
    String token,
    long expiresIn,
    TenantInfo tenant
) {
    public record TenantInfo(Long id, String name) {}
}
