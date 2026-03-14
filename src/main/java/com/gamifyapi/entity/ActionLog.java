package com.gamifyapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Registro imutável de cada ação executada por um player.
 * Uma vez criado, nunca é alterado.
 * O XP concedido é o valor da ActionDefinition no momento da execução.
 */
@Entity
@Table(
    name = "action_logs",
    indexes = {
        @Index(name = "idx_action_log_player_time", columnList = "player_id, timestamp DESC"),
        @Index(name = "idx_action_log_player_action", columnList = "player_id, action_definition_id")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_definition_id", nullable = false)
    private ActionDefinition actionDefinition;

    @Column(name = "xp_granted", nullable = false)
    private Integer xpGranted;

    @Column(nullable = false)
    private Instant timestamp;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionLog that = (ActionLog) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
