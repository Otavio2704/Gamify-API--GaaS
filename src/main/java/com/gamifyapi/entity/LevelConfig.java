package com.gamifyapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tabela de progressão de níveis configurada pelo tenant.
 * O nível 1 sempre deve ter xpRequired = 0.
 */
@Entity
@Table(
    name = "level_configs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_level_tenant_level",
        columnNames = {"tenant_id", "level"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "xp_required", nullable = false)
    private Integer xpRequired;

    @Column(length = 50)
    private String title;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LevelConfig that = (LevelConfig) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
