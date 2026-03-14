package com.gamifyapi.entity;

import com.gamifyapi.enums.RankingPeriod;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entrada no ranking/leaderboard de um tenant.
 * Armazena posição, score e período para consultas eficientes.
 */
@Entity
@Table(
    name = "ranking_entries",
    indexes = {
        @Index(name = "idx_ranking_lookup",
               columnList = "tenant_id, period, period_key, position")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RankingPeriod period;

    /**
     * Chave do período. Ex: "GLOBAL", "2025-W03", "2025-01".
     */
    @Column(name = "period_key", nullable = false, length = 10)
    private String periodKey;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RankingEntry that = (RankingEntry) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
