package com.gamifyapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Usuário final do app cliente dentro da GamifyAPI.
 * Identificado pelo externalId (ID do sistema do cliente) + tenantId.
 * Nunca exposto diretamente — sempre via DTO.
 */
@Entity
@Table(
    name = "players",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_player_tenant_external",
        columnNames = {"tenant_id", "external_id"}
    ),
    indexes = {
        @Index(name = "idx_player_xp", columnList = "tenant_id, total_xp DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(length = 100)
    private String displayName;

    @Column(name = "total_xp", nullable = false)
    @Builder.Default
    private Integer totalXp = 0;

    @Column(name = "current_level", nullable = false)
    @Builder.Default
    private Integer currentLevel = 1;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Atalho para obter o ID do tenant sem navegar pela associação lazy.
     */
    public Long getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    /**
     * Incrementa o XP total do player.
     * XP é cumulativo e nunca decresce.
     *
     * @param xp quantidade de XP a adicionar (deve ser positivo)
     */
    public void adicionarXp(int xp) {
        this.totalXp += xp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id != null && id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
