package com.gamifyapi.dto.response;

import com.gamifyapi.enums.WebhookEventType;

import java.time.LocalDateTime;

public record WebhookConfigResponse(
    Long id,
    String url,
    WebhookEventType eventType,
    Boolean active,
    LocalDateTime createdAt
) {}
