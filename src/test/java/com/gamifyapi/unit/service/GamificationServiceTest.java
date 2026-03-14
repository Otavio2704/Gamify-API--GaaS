package com.gamifyapi.unit.service;

import com.gamifyapi.achievement.AchievementEngine;
import com.gamifyapi.dto.request.ProcessActionRequest;
import com.gamifyapi.dto.response.ActionResultResponse;
import com.gamifyapi.entity.ActionDefinition;
import com.gamifyapi.entity.Player;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.exception.RecursoNaoEncontradoException;
import com.gamifyapi.repository.ActionDefinitionRepository;
import com.gamifyapi.security.TenantContext;
import com.gamifyapi.service.*;
import com.gamifyapi.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.gamifyapi.dto.response.ActionResultResponse.LevelUpDetails;
import static com.gamifyapi.dto.response.ActionResultResponse.StreakInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GamificationService — orquestração do fluxo principal")
class GamificationServiceTest {

    @Mock private PlayerService playerService;
    @Mock private CooldownService cooldownService;
    @Mock private XpService xpService;
    @Mock private LevelService levelService;
    @Mock private StreakService streakService;
    @Mock private AchievementEngine achievementEngine;
    @Mock private RankingService rankingService;
    @Mock private WebhookService webhookService;
    @Mock private ActionDefinitionRepository actionDefinitionRepository;

    @InjectMocks
    private GamificationService gamificationService;

    private Tenant tenant;
    private Player player;
    private ActionDefinition acao;

    @BeforeEach
    void configurar() {
        tenant = TestDataFactory.umTenant();
        player = TestDataFactory.umPlayer();
        acao = TestDataFactory.umaAcao();
        TenantContext.setTenant(tenant);
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("should_RetornarResultadoCompleto_When_AcaoProcessadaComSucesso")
    void should_RetornarResultadoCompleto_When_AcaoProcessadaComSucesso() {
        ProcessActionRequest request = new ProcessActionRequest("user-123", "João", "login_diario");

        when(playerService.buscarOuCriar(any(), anyString(), anyString())).thenReturn(player);
        when(actionDefinitionRepository.findByTenantIdAndCodeAndActiveTrue(anyLong(), anyString()))
            .thenReturn(Optional.of(acao));
        doNothing().when(cooldownService).validar(any(), any());
        when(xpService.concederXp(any(), any())).thenReturn(10);
        when(levelService.verificarLevelUp(any())).thenReturn(LevelUpDetails.nenhum(1));
        when(streakService.atualizar(any())).thenReturn(new StreakInfo(1, 1, false));
        when(achievementEngine.avaliar(any())).thenReturn(List.of());
        when(rankingService.atualizarEObterPosicao(any())).thenReturn(5);
        doNothing().when(webhookService).dispararAsync(any(), any(), any(), any(), any());

        ActionResultResponse resultado = gamificationService.processarAcao(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.action()).isEqualTo("login_diario");
        assertThat(resultado.xpGranted()).isEqualTo(10);
        assertThat(resultado.leaderboardPosition()).isEqualTo(5);
        assertThat(resultado.newAchievements()).isEmpty();
    }

    @Test
    @DisplayName("should_LancarNotFoundException_When_AcaoInexistente")
    void should_LancarNotFoundException_When_AcaoInexistente() {
        ProcessActionRequest request = new ProcessActionRequest("user-123", null, "acao_invalida");

        when(playerService.buscarOuCriar(any(), anyString(), any())).thenReturn(player);
        when(actionDefinitionRepository.findByTenantIdAndCodeAndActiveTrue(anyLong(), anyString()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.processarAcao(request))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("should_IncluirLevelUp_When_PlayerSubiuDeNivel")
    void should_IncluirLevelUp_When_PlayerSubiuDeNivel() {
        ProcessActionRequest request = new ProcessActionRequest("user-123", "João", "login_diario");

        when(playerService.buscarOuCriar(any(), anyString(), anyString())).thenReturn(player);
        when(actionDefinitionRepository.findByTenantIdAndCodeAndActiveTrue(anyLong(), anyString()))
            .thenReturn(Optional.of(acao));
        doNothing().when(cooldownService).validar(any(), any());
        when(xpService.concederXp(any(), any())).thenReturn(100);
        when(levelService.verificarLevelUp(any())).thenReturn(LevelUpDetails.de(1, 2, "Guerreiro"));
        when(streakService.atualizar(any())).thenReturn(new StreakInfo(1, 1, false));
        when(achievementEngine.avaliar(any())).thenReturn(List.of());
        when(rankingService.atualizarEObterPosicao(any())).thenReturn(1);
        doNothing().when(webhookService).dispararAsync(any(), any(), any(), any(), any());

        ActionResultResponse resultado = gamificationService.processarAcao(request);

        assertThat(resultado.levelUp().happened()).isTrue();
        assertThat(resultado.levelUp().newLevel()).isEqualTo(2);
        assertThat(resultado.levelUp().title()).isEqualTo("Guerreiro");
    }

    @Test
    @DisplayName("should_ConcederXpBonus_When_ConquistaComRecompensa")
    void should_ConcederXpBonus_When_ConquistaComRecompensa() {
        ProcessActionRequest request = new ProcessActionRequest("user-123", "João", "login_diario");
        var conquista = TestDataFactory.umaConquistaActionCount(1);

        when(playerService.buscarOuCriar(any(), anyString(), anyString())).thenReturn(player);
        when(actionDefinitionRepository.findByTenantIdAndCodeAndActiveTrue(anyLong(), anyString()))
            .thenReturn(Optional.of(acao));
        doNothing().when(cooldownService).validar(any(), any());
        when(xpService.concederXp(any(), any())).thenReturn(10);
        when(levelService.verificarLevelUp(any())).thenReturn(LevelUpDetails.nenhum(1));
        when(streakService.atualizar(any())).thenReturn(new StreakInfo(1, 1, false));
        when(achievementEngine.avaliar(any())).thenReturn(List.of(conquista));
        when(achievementEngine.toUnlockedResponse(any())).thenReturn(
            new com.gamifyapi.dto.response.AchievementUnlockedResponse(
                conquista.getCode(), conquista.getName(), null, null, 50, java.time.Instant.now()));
        when(xpService.concederXpBonus(any(), eq(50))).thenReturn(60);
        when(rankingService.atualizarEObterPosicao(any())).thenReturn(3);
        doNothing().when(webhookService).dispararAsync(any(), any(), any(), any(), any());

        ActionResultResponse resultado = gamificationService.processarAcao(request);

        assertThat(resultado.newAchievements()).hasSize(1);
        verify(xpService).concederXpBonus(player, 50);
    }

    @Test
    @DisplayName("should_DispararWebhooks_When_AcaoProcessada")
    void should_DispararWebhooks_When_AcaoProcessada() {
        ProcessActionRequest request = new ProcessActionRequest("user-123", "João", "login_diario");

        when(playerService.buscarOuCriar(any(), anyString(), anyString())).thenReturn(player);
        when(actionDefinitionRepository.findByTenantIdAndCodeAndActiveTrue(anyLong(), anyString()))
            .thenReturn(Optional.of(acao));
        doNothing().when(cooldownService).validar(any(), any());
        when(xpService.concederXp(any(), any())).thenReturn(10);
        when(levelService.verificarLevelUp(any())).thenReturn(LevelUpDetails.nenhum(1));
        when(streakService.atualizar(any())).thenReturn(new StreakInfo(1, 1, false));
        when(achievementEngine.avaliar(any())).thenReturn(List.of());
        when(rankingService.atualizarEObterPosicao(any())).thenReturn(1);
        doNothing().when(webhookService).dispararAsync(any(), any(), any(), any(), any());

        gamificationService.processarAcao(request);

        verify(webhookService).dispararAsync(eq(tenant), eq(player), any(), any(), any());
    }
}
