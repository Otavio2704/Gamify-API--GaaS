package com.gamifyapi.repository;

import com.gamifyapi.entity.ActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {

    /** Último log de um player para verificação de cooldown. */
    Optional<ActionLog> findTopByPlayerIdAndActionDefinitionIdOrderByTimestampDesc(
        Long playerId, Long actionDefinitionId);

    /** Contagem para critério ACTION_COUNT. */
    @Query("SELECT COUNT(al) FROM ActionLog al " +
           "JOIN al.actionDefinition ad " +
           "WHERE al.player.id = :playerId AND ad.code = :code")
    long countByPlayerIdAndActionCode(@Param("playerId") Long playerId, @Param("code") String code);

    /** Verificação de existência para critério MULTI_ACTION. */
    @Query("SELECT COUNT(DISTINCT ad.code) FROM ActionLog al " +
           "JOIN al.actionDefinition ad " +
           "WHERE al.player.id = :playerId AND ad.code IN :codes")
    long countDistinctActionCodesForPlayer(
        @Param("playerId") Long playerId, @Param("codes") List<String> codes);

    /** Timeline do player paginada. */
    Page<ActionLog> findAllByPlayerIdOrderByTimestampDesc(Long playerId, Pageable pageable);

    /** Contagem total de ações de um player. */
    long countByPlayerId(Long playerId);

    /** Contagem total de ações de um tenant. */
    @Query("SELECT COUNT(al) FROM ActionLog al WHERE al.player.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);

    /** Contagem de ações de um tenant em um período. */
    @Query("SELECT COUNT(al) FROM ActionLog al " +
           "WHERE al.player.tenant.id = :tenantId AND al.timestamp >= :desde")
    long countByTenantIdSince(@Param("tenantId") Long tenantId, @Param("desde") Instant desde);

    /** Ações por período para o ranking semanal/mensal. */
    @Query("SELECT al FROM ActionLog al " +
           "WHERE al.player.tenant.id = :tenantId " +
           "AND al.timestamp >= :inicio AND al.timestamp < :fim")
    List<ActionLog> findByTenantIdAndPeriod(
        @Param("tenantId") Long tenantId,
        @Param("inicio") Instant inicio,
        @Param("fim") Instant fim);

    /** XP de um player em um período (ranking temporal). */
    @Query("SELECT COALESCE(SUM(al.xpGranted), 0) FROM ActionLog al " +
           "WHERE al.player.id = :playerId " +
           "AND al.timestamp >= :inicio AND al.timestamp < :fim")
    int sumXpByPlayerAndPeriod(
        @Param("playerId") Long playerId,
        @Param("inicio") Instant inicio,
        @Param("fim") Instant fim);

    /** Top action de um tenant. */
    @Query("SELECT ad.code, COUNT(al) as cnt FROM ActionLog al " +
           "JOIN al.actionDefinition ad " +
           "WHERE al.player.tenant.id = :tenantId " +
           "GROUP BY ad.code ORDER BY cnt DESC")
    List<Object[]> findTopActionByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    /** Ações por dia para o gráfico do dashboard. */
    @Query("SELECT CAST(al.timestamp AS date), COUNT(al) FROM ActionLog al " +
           "WHERE al.player.tenant.id = :tenantId AND al.timestamp >= :desde " +
           "GROUP BY CAST(al.timestamp AS date) ORDER BY CAST(al.timestamp AS date)")
    List<Object[]> countByTenantIdGroupByDay(
        @Param("tenantId") Long tenantId, @Param("desde") Instant desde);
}
