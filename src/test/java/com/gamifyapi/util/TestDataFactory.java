package com.gamifyapi.util;

import com.gamifyapi.entity.*;
import com.gamifyapi.enums.CriteriaType;
import com.gamifyapi.enums.RankingPeriod;
import com.gamifyapi.enums.TenantPlan;
import com.gamifyapi.enums.WebhookEventType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fábrica de dados de teste para uso nos testes unitários e de integração.
 */
public class TestDataFactory {

    // -------------------------------------------------------------------------
    // Tenant
    // -------------------------------------------------------------------------

    public static Tenant umTenant() {
        Tenant t = new Tenant();
        t.setId(1L);
        t.setName("Tenant Teste");
        t.setEmail("tenant@test.com");
        t.setPasswordHash("$2a$10$hashfake");
        t.setPlan(TenantPlan.FREE);
        return t;
    }

    public static Tenant umTenantComId(Long id) {
        Tenant t = umTenant();
        t.setId(id);
        return t;
    }

    // -------------------------------------------------------------------------
    // Player
    // -------------------------------------------------------------------------

    public static Player umPlayer() {
        Player p = new Player();
        p.setId(1L);
        p.setTenant(umTenant());
        p.setExternalId("user-123");
        p.setDisplayName("João Silva");
        p.setTotalXp(0);
        p.setCurrentLevel(1);
        p.setCurrentStreak(0);
        p.setLongestStreak(0);
        p.setLastActivityDate(null);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    public static Player umPlayerComXp(int xp) {
        Player p = umPlayer();
        p.setTotalXp(xp);
        return p;
    }

    public static Player umPlayerComNivel(int nivel) {
        Player p = umPlayer();
        p.setCurrentLevel(nivel);
        return p;
    }

    public static Player umPlayerComStreak(int streak) {
        Player p = umPlayer();
        p.setCurrentStreak(streak);
        p.setLongestStreak(streak);
        return p;
    }

    public static Player umPlayerComUltimaAtividade(LocalDate data) {
        Player p = umPlayer();
        p.setLastActivityDate(data);
        return p;
    }

    // -------------------------------------------------------------------------
    // ActionDefinition
    // -------------------------------------------------------------------------

    public static ActionDefinition umaAcao() {
        ActionDefinition a = new ActionDefinition();
        a.setId(1L);
        a.setTenant(umTenant());
        a.setCode("login_diario");
        a.setDisplayName("Login Diário");
        a.setXpValue(10);
        a.setCooldownSeconds(0);
        a.setActive(true);
        return a;
    }

    public static ActionDefinition umaAcaoComCooldown(int segundos) {
        ActionDefinition a = umaAcao();
        a.setCooldownSeconds(segundos);
        return a;
    }

    public static ActionDefinition umaAcaoComXp(int xp) {
        ActionDefinition a = umaAcao();
        a.setXpValue(xp);
        return a;
    }

    // -------------------------------------------------------------------------
    // LevelConfig
    // -------------------------------------------------------------------------

    public static LevelConfig umNivelConfig(int nivel, int xpRequerido) {
        LevelConfig lc = new LevelConfig();
        lc.setId((long) nivel);
        lc.setTenant(umTenant());
        lc.setLevel(nivel);
        lc.setXpRequired(xpRequerido);
        lc.setTitle("Nível " + nivel);
        return lc;
    }

    // -------------------------------------------------------------------------
    // Achievement
    // -------------------------------------------------------------------------

    public static Achievement umaConquistaActionCount(int count) {
        Achievement a = new Achievement();
        a.setId(1L);
        a.setTenant(umTenant());
        a.setCode("PRIMEIRO_LOGIN");
        a.setName("Primeiro Login");
        a.setDescription("Fez login pela primeira vez");
        a.setXpReward(50);
        a.setCriteriaType(CriteriaType.ACTION_COUNT);
        a.setCriteriaValue("{\"action\": \"login_diario\", \"count\": " + count + "}");
        a.setActive(true);
        return a;
    }

    public static Achievement umaConquistaStreak(int minStreak) {
        Achievement a = new Achievement();
        a.setId(2L);
        a.setTenant(umTenant());
        a.setCode("STREAK_7");
        a.setName("Streak de 7 dias");
        a.setDescription("Jogou 7 dias seguidos");
        a.setXpReward(100);
        a.setCriteriaType(CriteriaType.STREAK);
        a.setCriteriaValue("{\"minStreak\": " + minStreak + "}");
        a.setActive(true);
        return a;
    }

    public static Achievement umaConquistaLevel(int nivel) {
        Achievement a = new Achievement();
        a.setId(3L);
        a.setTenant(umTenant());
        a.setCode("NIVEL_5");
        a.setName("Nível 5 Alcançado");
        a.setDescription("Atingiu o nível 5");
        a.setXpReward(200);
        a.setCriteriaType(CriteriaType.LEVEL_REACHED);
        a.setCriteriaValue("{\"level\": " + nivel + "}");
        a.setActive(true);
        return a;
    }

    // -------------------------------------------------------------------------
    // ActionLog
    // -------------------------------------------------------------------------

    public static ActionLog umActionLog(Player player, ActionDefinition acao, int xp) {
        return ActionLog.builder()
            .id(1L)
            .player(player)
            .actionDefinition(acao)
            .xpGranted(xp)
            .timestamp(Instant.now())
            .build();
    }

    public static ActionLog umActionLogComTimestamp(Player player, ActionDefinition acao,
                                                    int xp, Instant timestamp) {
        return ActionLog.builder()
            .id(1L)
            .player(player)
            .actionDefinition(acao)
            .xpGranted(xp)
            .timestamp(timestamp)
            .build();
    }
}
