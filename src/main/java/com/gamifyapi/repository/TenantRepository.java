package com.gamifyapi.repository;

import com.gamifyapi.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByEmail(String email);

    boolean existsByEmail(String email);
}
