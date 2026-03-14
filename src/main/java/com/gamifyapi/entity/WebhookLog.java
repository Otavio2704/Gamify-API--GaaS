package com.gamifyapi.entity;

import com.gamifyapi.enums.WebhookEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Registro de cada tentativa de disparo de webhook.
 * Armazena payload, status HTTP e número de tentativas para debug e retry.
 */
@Entity
@Table(name = "webhook_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_config_id", nullable = false)
    private WebhookConfig webhookConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private WebhookEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 1;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebhookLog that = (WebhookLog) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
