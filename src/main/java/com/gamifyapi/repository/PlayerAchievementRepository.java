package com.gamifyapi.repository;

import com.gamifyapi.entity.PlayerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {

    List<PlayerAchievement> findAllByPlayerId(Long playerId);

    boolean existsByPlayerIdAndAchievementId(Long playerId, Long achievementId);

    long countByPlayerIdIn(List<Long> playerIds);

    @Query("SELECT COUNT(pa) FROM PlayerAchievement pa WHERE pa.player.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(pa) FROM PlayerAchievement pa WHERE pa.player.id = :playerId")
    long countByPlayerId(@Param("playerId") Long playerId);
}
