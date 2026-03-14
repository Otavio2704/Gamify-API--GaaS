package com.gamifyapi.repository;

import com.gamifyapi.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findAllByTenantId(Long tenantId);

    List<Achievement> findAllByTenantIdAndActiveTrue(Long tenantId);

    boolean existsByTenantIdAndCode(Long tenantId, String code);

    @Query("SELECT a FROM Achievement a WHERE a.id = :id AND a.tenant.id = :tenantId")
    Optional<Achievement> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * Retorna conquistas ativas que o player ainda não desbloqueou.
     */
    @Query("SELECT a FROM Achievement a " +
           "WHERE a.tenant.id = :tenantId AND a.active = true " +
           "AND a.id NOT IN (" +
           "  SELECT pa.achievement.id FROM PlayerAchievement pa WHERE pa.player.id = :playerId" +
           ")")
    List<Achievement> findAtivasNaoDesbloqueasPorPlayer(
        @Param("tenantId") Long tenantId, @Param("playerId") Long playerId);
}
