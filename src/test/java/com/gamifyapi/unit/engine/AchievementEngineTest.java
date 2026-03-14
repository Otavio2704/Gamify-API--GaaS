package com.gamifyapi.unit.engine;

import com.gamifyapi.achievement.*;
import com.gamifyapi.entity.Achievement;
import com.gamifyapi.entity.Player;
import com.gamifyapi.enums.CriteriaType;
import com.gamifyapi.repository.AchievementRepository;
import com.gamifyapi.repository.PlayerAchievementRepository;
import com.gamifyapi.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AchievementEngine — avaliação e desbloqueio de conquistas")
class AchievementEngineTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private PlayerAchievementRepository playerAchievementRepository;

    @Mock
    private EvaluatorFactory evaluatorFactory;

    @InjectMocks
    private AchievementEngine achievementEngine;

    @Test
    @DisplayName("should_DesbloquearConquista_When_CriterioAtendido")
    void should_DesbloquearConquista_When_CriterioAtendido() {
        Player player = TestDataFactory.umPlayer();
        Achievement conquista = TestDataFactory.umaConquistaActionCount(1);

        when(achievementRepository.findAtivasNaoDesbloqueasPorPlayer(anyLong(), anyLong()))
            .thenReturn(List.of(conquista));

        AchievementCriteriaEvaluator evaluator = (p, a) -> true;
        when(evaluatorFactory.get(CriteriaType.ACTION_COUNT)).thenReturn(evaluator);
        when(playerAchievementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Achievement> desbloqueadas = achievementEngine.avaliar(player);

        assertThat(desbloqueadas).hasSize(1);
        assertThat(desbloqueadas.get(0).getCode()).isEqualTo("PRIMEIRO_LOGIN");
    }

    @Test
    @DisplayName("should_NaoDesbloquear_When_CriterioNaoAtendido")
    void should_NaoDesbloquear_When_CriterioNaoAtendido() {
        Player player = TestDataFactory.umPlayer();
        Achievement conquista = TestDataFactory.umaConquistaActionCount(10);

        when(achievementRepository.findAtivasNaoDesbloqueasPorPlayer(anyLong(), anyLong()))
            .thenReturn(List.of(conquista));

        AchievementCriteriaEvaluator evaluator = (p, a) -> false;
        when(evaluatorFactory.get(CriteriaType.ACTION_COUNT)).thenReturn(evaluator);

        List<Achievement> desbloqueadas = achievementEngine.avaliar(player);

        assertThat(desbloqueadas).isEmpty();
        verify(playerAchievementRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_NaoAvaliar_When_ConquistaJaDesbloqueada")
    void should_NaoAvaliar_When_ConquistaJaDesbloqueada() {
        Player player = TestDataFactory.umPlayer();

        // findAtivasNaoDesbloqueasPorPlayer já filtra as desbloqueadas no DB
        when(achievementRepository.findAtivasNaoDesbloqueasPorPlayer(anyLong(), anyLong()))
            .thenReturn(List.of());

        List<Achievement> desbloqueadas = achievementEngine.avaliar(player);

        assertThat(desbloqueadas).isEmpty();
        verifyNoInteractions(evaluatorFactory);
    }

    @Test
    @DisplayName("should_AvaliarCriterioStreak_When_TipoStreak")
    void should_AvaliarCriterioStreak_When_TipoStreak() {
        Player player = TestDataFactory.umPlayerComStreak(7);
        Achievement conquista = TestDataFactory.umaConquistaStreak(7);

        when(achievementRepository.findAtivasNaoDesbloqueasPorPlayer(anyLong(), anyLong()))
            .thenReturn(List.of(conquista));

        AchievementCriteriaEvaluator evaluator = (p, a) -> p.getCurrentStreak() >= 7;
        when(evaluatorFactory.get(CriteriaType.STREAK)).thenReturn(evaluator);
        when(playerAchievementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Achievement> desbloqueadas = achievementEngine.avaliar(player);

        assertThat(desbloqueadas).hasSize(1);
    }

    @Test
    @DisplayName("should_AvaliarConquistaLevel_When_TipoLevelReached")
    void should_AvaliarConquistaLevel_When_TipoLevelReached() {
        Player player = TestDataFactory.umPlayerComNivel(5);
        Achievement conquista = TestDataFactory.umaConquistaLevel(5);

        when(achievementRepository.findAtivasNaoDesbloqueasPorPlayer(anyLong(), anyLong()))
            .thenReturn(List.of(conquista));

        AchievementCriteriaEvaluator evaluator = (p, a) -> p.getCurrentLevel() >= 5;
        when(evaluatorFactory.get(CriteriaType.LEVEL_REACHED)).thenReturn(evaluator);
        when(playerAchievementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Achievement> desbloqueadas = achievementEngine.avaliar(player);

        assertThat(desbloqueadas).hasSize(1);
    }

    @Test
    @DisplayName("should_ContinuarAvaliandoOutras_When_EvaluatorLancaExcecao")
    void should_ContinuarAvaliandoOutras_When_EvaluatorLancaExcecao() {
        Player player = TestDataFactory.umPlayer();
        Achievement conquista1 = TestDataFactory.umaConquistaActionCount(1);
        Achievement conquista2 = TestDataFactory.umaConquistaStreak(7);
        conquista2.setId(99L);

        when(achievementRepository.findAtivasNaoDesbloqueasPorPlayer(anyLong(), anyLong()))
            .thenReturn(List.of(conquista1, conquista2));

        when(evaluatorFactory.get(CriteriaType.ACTION_COUNT))
            .thenThrow(new RuntimeException("Erro simulado"));

        AchievementCriteriaEvaluator evaluadorOk = (p, a) -> true;
        when(evaluatorFactory.get(CriteriaType.STREAK)).thenReturn(evaluadorOk);
        when(playerAchievementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Achievement> desbloqueadas = achievementEngine.avaliar(player);

        // conquista1 falhou, conquista2 foi desbloqueada
        assertThat(desbloqueadas).hasSize(1);
        assertThat(desbloqueadas.get(0).getCode()).isEqualTo("STREAK_7");
    }
}
