package com.gamifyapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Relação N:N entre Player e Achievement.
 * Registra quando o player desbloqueou a conquista.
 * Uma conquista só pode ser desbloqueada uma vez por player.
 */
@Entity
@Table(
    name = "player_achievements",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_player_achievement",
        columnNames = {"player_id", "achievement_id"}
    )
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerAchievement that = (PlayerAchievement) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
