package com.gamifyapi.dto.response;

import com.gamifyapi.enums.CriteriaType;

import java.time.LocalDateTime;

public record AchievementResponse(
    Long id,
    String code,
    String name,
    String description,
    String badgeImageUrl,
    Integer xpReward,
    CriteriaType criteriaType,
    Object criteriaValue,
    Boolean active,
    LocalDateTime createdAt
) {}
