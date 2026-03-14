package com.gamifyapi.repository;

import com.gamifyapi.entity.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByTenant_IdAndExternalId(Long tenantId, String externalId);

    long countByTenant_Id(Long tenantId);

    long countByTenant_IdAndLastActivityDateAfter(Long tenantId, LocalDate after);

    @Query("SELECT AVG(p.currentLevel) FROM Player p WHERE p.tenant.id = :tenantId")
    Double findAverageLevelByTenantId(@Param("tenantId") Long tenantId);

    /** Top players por XP para o leaderboard global. */
    Page<Player> findAllByTenant_IdOrderByTotalXpDescCreatedAtAsc(Long tenantId, Pageable pageable);

    List<Player> findTop5ByTenant_IdOrderByTotalXpDesc(Long tenantId);
}
