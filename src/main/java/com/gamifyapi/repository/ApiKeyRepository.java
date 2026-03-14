package com.gamifyapi.repository;

import com.gamifyapi.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /** Busca por hash SHA-256 da chave — usado na autenticação. */
    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);

    List<ApiKey> findAllByTenantId(Long tenantId);

    @Query("SELECT k FROM ApiKey k WHERE k.id = :id AND k.tenant.id = :tenantId")
    Optional<ApiKey> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
