package com.gamifyapi.service;

import com.gamifyapi.dto.request.LoginRequest;
import com.gamifyapi.dto.request.RegisterRequest;
import com.gamifyapi.dto.response.AuthResponse;
import com.gamifyapi.dto.response.TenantResponse;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.exception.AutenticacaoException;
import com.gamifyapi.exception.ConflitoException;
import com.gamifyapi.repository.TenantRepository;
import com.gamifyapi.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de autenticação e cadastro de tenants.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registra um novo tenant.
     *
     * @throws ConflitoException se o e-mail já estiver cadastrado
     */
    @Transactional
    public TenantResponse registrar(RegisterRequest request) {
        if (tenantRepository.existsByEmail(request.email())) {
            throw new ConflitoException("E-mail já cadastrado: " + request.email());
        }

        Tenant tenant = Tenant.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Novo tenant cadastrado: {} ({})", tenant.getName(), tenant.getEmail());

        return toResponse(tenant);
    }

    /**
     * Autentica o tenant e retorna o JWT.
     *
     * @throws AutenticacaoException se as credenciais forem inválidas
     */
    public AuthResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findByEmail(request.email())
            .orElseThrow(() -> new AutenticacaoException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.password(), tenant.getPasswordHash())) {
            throw new AutenticacaoException("Credenciais inválidas");
        }

        String token = jwtTokenProvider.gerarToken(tenant);
        long expiresIn = jwtTokenProvider.getExpiracaoMs() / 1000;

        return new AuthResponse(
            token,
            expiresIn,
            new AuthResponse.TenantInfo(tenant.getId(), tenant.getName())
        );
    }

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
            tenant.getId(),
            tenant.getName(),
            tenant.getEmail(),
            tenant.getPlan(),
            tenant.getCreatedAt()
        );
    }
}
