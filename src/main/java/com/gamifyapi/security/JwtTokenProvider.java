package com.gamifyapi.security;

import com.gamifyapi.entity.Tenant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Responsável por gerar e validar tokens JWT.
 * O subject (sub) do token contém o ID do tenant como string.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtTokenProvider(
            @Value("${gamify.jwt.secret}") String secret,
            @Value("${gamify.jwt.expiration-ms}") long expiracaoMs) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    /**
     * Gera um token JWT para o tenant informado.
     */
    public String gerarToken(Tenant tenant) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(String.valueOf(tenant.getId()))
                .claim("email", tenant.getEmail())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    /**
     * Valida o token JWT.
     *
     * @return true se o token é válido
     */
    public boolean validarToken(String token) {
        try {
            Jwts.parser().verifyWith(chave).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token JWT inválido: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extrai o ID do tenant do token JWT.
     */
    public Long getTenantIdDoToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public long getExpiracaoMs() {
        return expiracaoMs;
    }
}
