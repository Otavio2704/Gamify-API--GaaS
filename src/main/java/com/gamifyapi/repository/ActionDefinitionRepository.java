package com.gamifyapi.repository;

import com.gamifyapi.entity.ActionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActionDefinitionRepository extends JpaRepository<ActionDefinition, Long> {

    Optional<ActionDefinition> findByTenantIdAndCodeAndActiveTrue(Long tenantId, String code);

    List<ActionDefinition> findAllByTenantId(Long tenantId);

    boolean existsByTenantIdAndCode(Long tenantId, String code);

    @Query("SELECT a FROM ActionDefinition a WHERE a.id = :id AND a.tenant.id = :tenantId")
    Optional<ActionDefinition> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
