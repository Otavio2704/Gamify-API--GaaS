package com.gamifyapi.enums;

/**
 * Tipos de evento que disparam webhooks.
 */
public enum WebhookEventType {
    /** Player subiu de nível. */
    LEVEL_UP,
    /** Player desbloqueou uma conquista. */
    ACHIEVEMENT_UNLOCKED,
    /** Player atingiu milestone de streak (7, 30, 100 dias). */
    STREAK_MILESTONE,
    /** Player mudou de posição no ranking. */
    RANK_CHANGED
}
