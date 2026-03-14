package com.gamifyapi.unit.service;

import com.gamifyapi.entity.ActionDefinition;
import com.gamifyapi.entity.ActionLog;
import com.gamifyapi.entity.Player;
import com.gamifyapi.exception.CooldownAtivoException;
import com.gamifyapi.repository.ActionLogRepository;
import com.gamifyapi.service.CooldownService;
import com.gamifyapi.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CooldownService — validação de cooldown de ações")
class CooldownServiceTest {

    @Mock
    private ActionLogRepository actionLogRepository;

    @InjectMocks
    private CooldownService cooldownService;

    @Test
    @DisplayName("should_NaoLancarExcecao_When_CooldownZero")
    void should_NaoLancarExcecao_When_CooldownZero() {
        Player player = TestDataFactory.umPlayer();
        ActionDefinition acao = TestDataFactory.umaAcaoComCooldown(0);

        assertThatCode(() -> cooldownService.validar(player, acao))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_NaoLancarExcecao_When_PrimeiraVezRealizandoAcao")
    void should_NaoLancarExcecao_When_PrimeiraVezRealizandoAcao() {
        Player player = TestDataFactory.umPlayer();
        ActionDefinition acao = TestDataFactory.umaAcaoComCooldown(3600);

        when(actionLogRepository.findTopByPlayerIdAndActionDefinitionIdOrderByTimestampDesc(
            anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatCode(() -> cooldownService.validar(player, acao))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_LancarCooldownAtivoException_When_DentroJanelaCooldown")
    void should_LancarCooldownAtivoException_When_DentroJanelaCooldown() {
        Player player = TestDataFactory.umPlayer();
        ActionDefinition acao = TestDataFactory.umaAcaoComCooldown(3600); // 1 hora

        ActionLog ultimaExecucao = TestDataFactory.umActionLogComTimestamp(
            player, acao, 10, Instant.now().minusSeconds(100)); // há 100 segundos

        when(actionLogRepository.findTopByPlayerIdAndActionDefinitionIdOrderByTimestampDesc(
            anyLong(), anyLong())).thenReturn(Optional.of(ultimaExecucao));

        assertThatThrownBy(() -> cooldownService.validar(player, acao))
            .isInstanceOf(CooldownAtivoException.class);
    }

    @Test
    @DisplayName("should_NaoLancarExcecao_When_CooldownExpirado")
    void should_NaoLancarExcecao_When_CooldownExpirado() {
        Player player = TestDataFactory.umPlayer();
        ActionDefinition acao = TestDataFactory.umaAcaoComCooldown(3600); // 1 hora

        ActionLog ultimaExecucao = TestDataFactory.umActionLogComTimestamp(
            player, acao, 10, Instant.now().minusSeconds(7200)); // há 2 horas

        when(actionLogRepository.findTopByPlayerIdAndActionDefinitionIdOrderByTimestampDesc(
            anyLong(), anyLong())).thenReturn(Optional.of(ultimaExecucao));

        assertThatCode(() -> cooldownService.validar(player, acao))
            .doesNotThrowAnyException();
    }
}
