package com.gamifyapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Template de ação configurado pelo tenant.
 * Define o código da ação, XP concedido e cooldown entre execuções.
 */
@Entity
@Table(
    name = "action_definitions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_action_tenant_code",
        columnNames = {"tenant_id", "code"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 255)
    private String description;

    @Column(name = "xp_value", nullable = false)
    private Integer xpValue;

    @Column(name = "cooldown_seconds", nullable = false)
    @Builder.Default
    private Integer cooldownSeconds = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionDefinition that = (ActionDefinition) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
