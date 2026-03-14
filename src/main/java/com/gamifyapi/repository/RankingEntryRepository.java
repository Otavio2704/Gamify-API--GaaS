package com.gamifyapi.repository;

import com.gamifyapi.entity.RankingEntry;
import com.gamifyapi.enums.RankingPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RankingEntryRepository extends JpaRepository<RankingEntry, Long> {

    Page<RankingEntry> findAllByTenantIdAndPeriodAndPeriodKeyOrderByPositionAsc(
            Long tenantId, RankingPeriod period, String periodKey, Pageable pageable);

    long countByTenantIdAndPeriodAndPeriodKey(
            Long tenantId, RankingPeriod period, String periodKey);

    @Query("SELECT r FROM RankingEntry r " +
           "WHERE r.tenant.id = :tenantId AND r.player.id = :playerId " +
           "AND r.period = :period AND r.periodKey = :periodKey")
    Optional<RankingEntry> findByTenantPlayerAndPeriod(
            @Param("tenantId")  Long          tenantId,
            @Param("playerId")  Long          playerId,
            @Param("period")    RankingPeriod period,
            @Param("periodKey") String        periodKey);

    /**
     * Recalcula posições de todos os players no período diretamente no banco,
     * usando ROW_NUMBER() — evita carregar todos os registros em memória.
     * Compatível com PostgreSQL e H2 2.x em MODE=PostgreSQL.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE ranking_entries re
               SET position   = ranked.new_pos,
                   updated_at = CAST(:now AS timestamp)
              FROM (
                       SELECT id,
                              ROW_NUMBER() OVER (ORDER BY score DESC) AS new_pos
                         FROM ranking_entries
                        WHERE tenant_id  = :tenantId
                          AND period     = :period
                          AND period_key = :periodKey
                   ) ranked
             WHERE re.id = ranked.id
            """, nativeQuery = true)
    void recalcularPosicoes(
            @Param("tenantId")  Long    tenantId,
            @Param("period")    String  period,
            @Param("periodKey") String  periodKey,
            @Param("now")       Instant now);
}
