package com.gamifyapi.repository;

import com.gamifyapi.entity.WebhookConfig;
import com.gamifyapi.enums.WebhookEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    List<WebhookConfig> findAllByTenantId(Long tenantId);

    List<WebhookConfig> findAllByTenantIdAndEventTypeAndActiveTrue(
        Long tenantId, WebhookEventType eventType);

    @Query("SELECT w FROM WebhookConfig w WHERE w.id = :id AND w.tenant.id = :tenantId")
    Optional<WebhookConfig> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
