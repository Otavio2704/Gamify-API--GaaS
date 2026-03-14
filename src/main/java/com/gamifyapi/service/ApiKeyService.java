package com.gamifyapi.service;

import com.gamifyapi.dto.request.CreateApiKeyRequest;
import com.gamifyapi.dto.response.ApiKeyResponse;
import com.gamifyapi.entity.ApiKey;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.exception.RecursoNaoEncontradoException;
import com.gamifyapi.repository.ApiKeyRepository;
import com.gamifyapi.security.ApiKeyAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * Gerencia a criação, listagem e remoção de API Keys dos tenants.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private static final String KEY_PREFIX = "gapi_";
    private static final int KEY_BYTES = 32;

    private final ApiKeyRepository apiKeyRepository;

    /**
     * Cria uma nova API Key para o tenant.
     * A chave raw é retornada SOMENTE neste momento — depois, apenas o prefix.
     */
    @Transactional
    public ApiKeyResponse criar(Tenant tenant, CreateApiKeyRequest request) {
        String rawKey = gerarChave();
        String hash = ApiKeyAuthenticationFilter.hashSHA256(rawKey);
        String prefix = rawKey.substring(0, Math.min(rawKey.length(), 12));

        ApiKey apiKey = ApiKey.builder()
                .tenant(tenant)
                .keyHash(hash)
                .prefix(prefix)
                .label(request.label())
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        log.info("API Key criada para tenant '{}': prefix={}", tenant.getId(), prefix);

        return new ApiKeyResponse(
            apiKey.getId(), rawKey, prefix,
            apiKey.getLabel(), apiKey.getActive(), apiKey.getCreatedAt()
        );
    }

    /**
     * Lista todas as API Keys do tenant (sem trazer a key raw ou hash).
     */
    public List<ApiKeyResponse> listarPorTenant(Long tenantId) {
        return apiKeyRepository.findAllByTenantId(tenantId).stream()
            .map(k -> new ApiKeyResponse(
                k.getId(), null, k.getPrefix(),
                k.getLabel(), k.getActive(), k.getCreatedAt()))
            .toList();
    }

    /**
     * Remove (desativa) uma API Key do tenant.
     *
     * @throws RecursoNaoEncontradoException se não encontrada para este tenant
     */
    @Transactional
    public void remover(Long tenantId, Long id) {
        ApiKey apiKey = apiKeyRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("API Key", id));
        apiKeyRepository.delete(apiKey);
    }

    private String gerarChave() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[KEY_BYTES];
        random.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
