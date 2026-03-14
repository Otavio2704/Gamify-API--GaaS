package com.gamifyapi.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamifyapi.enums.CriteriaType;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Conquista/badge configurada pelo tenant.
 * O criteriaValue é armazenado como JSON e interpretado conforme criteriaType.
 */
@Entity
@Table(
    name = "achievements",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_achievement_tenant_code",
        columnNames = {"tenant_id", "code"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class Achievement {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "badge_image_url", length = 500)
    private String badgeImageUrl;

    @Column(name = "xp_reward", nullable = false)
    @Builder.Default
    private Integer xpReward = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_type", nullable = false, length = 30)
    private CriteriaType criteriaType;

    /**
     * JSON com os valores do critério. Exemplos:
     * ACTION_COUNT: {"actionCode":"completed_lesson","count":50}
     * STREAK:       {"minStreak":7}
     * LEVEL_REACHED:{"level":10}
     * XP_TOTAL:     {"minXp":5000}
     * MULTI_ACTION: {"actionCodes":["completed_lesson","passed_quiz"]}
     */
    @Column(name = "criteria_value", nullable = false, columnDefinition = "TEXT")
    private String criteriaValue;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Retorna o criteriaValue deserializado como Map.
     */
    public Map<String, Object> getCriteriaValueAsMap() {
        try {
            return MAPPER.readValue(criteriaValue, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Erro ao deserializar criteriaValue: {}", criteriaValue, e);
            return Map.of();
        }
    }

    /**
     * Retorna o valor inteiro de uma chave do JSON de critério.
     */
    public int getCriteriaValueAsInt(String key) {
        Object val = getCriteriaValueAsMap().get(key);
        if (val instanceof Number n) return n.intValue();
        throw new IllegalStateException("Chave '%s' não encontrada ou não é número em: %s".formatted(key, criteriaValue));
    }

    /**
     * Retorna a lista de strings de uma chave do JSON de critério.
     */
    @SuppressWarnings("unchecked")
    public List<String> getCriteriaValueAsList(String key) {
        Object val = getCriteriaValueAsMap().get(key);
        if (val instanceof List<?> list) return (List<String>) list;
        throw new IllegalStateException("Chave '%s' não encontrada ou não é lista em: %s".formatted(key, criteriaValue));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Achievement that = (Achievement) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
