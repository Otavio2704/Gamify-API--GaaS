package com.gamifyapi.repository;

import com.gamifyapi.entity.LevelConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LevelConfigRepository extends JpaRepository<LevelConfig, Long> {

    List<LevelConfig> findAllByTenantIdOrderByLevelAsc(Long tenantId);

    void deleteAllByTenantId(Long tenantId);
}
