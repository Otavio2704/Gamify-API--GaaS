package com.gamifyapi.repository;

import com.gamifyapi.entity.RankingEntry;
import com.gamifyapi.enums.RankingPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RankingEntryRepository extends JpaRepository<RankingEntry, Long> {

    Page<RankingEntry> findAllByTenantIdAndPeriodAndPeriodKeyOrderByPositionAsc(
        Long tenantId, RankingPeriod period, String periodKey, Pageable pageable);

    long countByTenantIdAndPeriodAndPeriodKey(Long tenantId, RankingPeriod period, String periodKey);

    @Query("SELECT r FROM RankingEntry r " +
           "WHERE r.tenant.id = :tenantId AND r.player.id = :playerId " +
           "AND r.period = :period AND r.periodKey = :periodKey")
    Optional<RankingEntry> findByTenantPlayerAndPeriod(
        @Param("tenantId") Long tenantId,
        @Param("playerId") Long playerId,
        @Param("period") RankingPeriod period,
        @Param("periodKey") String periodKey);
}
