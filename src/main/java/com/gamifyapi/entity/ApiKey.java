package com.gamifyapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Chave de API para integração de apps externos com a GamifyAPI.
 * A chave original nunca é armazenada — apenas o hash SHA-256.
 * O prefix (8 primeiros chars) permite identificação visual pelo admin.
 */
@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_api_keys_key_hash", columnList = "key_hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(nullable = false, length = 20)
    private String prefix;

    @Column(nullable = false, length = 50)
    private String label;

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
        ApiKey apiKey = (ApiKey) o;
        return id != null && id.equals(apiKey.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
