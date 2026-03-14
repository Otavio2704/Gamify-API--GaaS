package com.gamifyapi.unit.service;

import com.gamifyapi.entity.Player;
import com.gamifyapi.repository.PlayerRepository;
import com.gamifyapi.service.StreakService;
import com.gamifyapi.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreakService — regras de cálculo de streak")
class StreakServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private StreakService streakService;

    @Test
    @DisplayName("should_IniciarStreakEm1_When_PrimeiraAtividade")
    void should_IniciarStreakEm1_When_PrimeiraAtividade() {
        Player player = TestDataFactory.umPlayer();
        player.setLastActivityDate(null);
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var resultado = streakService.atualizar(player);

        assertThat(resultado.currentStreak()).isEqualTo(1);
        assertThat(resultado.wasReset()).isFalse();
        assertThat(player.getLastActivityDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("should_IncrementarStreak_When_AtividadeOntem")
    void should_IncrementarStreak_When_AtividadeOntem() {
        Player player = TestDataFactory.umPlayerComStreak(3);
        player.setLastActivityDate(LocalDate.now().minusDays(1));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var resultado = streakService.atualizar(player);

        assertThat(resultado.currentStreak()).isEqualTo(4);
        assertThat(resultado.wasReset()).isFalse();
    }

    @Test
    @DisplayName("should_NaoAlterarStreak_When_AtividadeHoje")
    void should_NaoAlterarStreak_When_AtividadeHoje() {
        Player player = TestDataFactory.umPlayerComStreak(5);
        player.setLastActivityDate(LocalDate.now());
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var resultado = streakService.atualizar(player);

        assertThat(resultado.currentStreak()).isEqualTo(5);
    }

    @Test
    @DisplayName("should_ResetarStreakParaUm_When_PulouDias")
    void should_ResetarStreakParaUm_When_PulouDias() {
        Player player = TestDataFactory.umPlayerComStreak(10);
        player.setLastActivityDate(LocalDate.now().minusDays(3));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var resultado = streakService.atualizar(player);

        assertThat(resultado.currentStreak()).isEqualTo(1);
        assertThat(resultado.wasReset()).isTrue();
    }

    @Test
    @DisplayName("should_AtualizarLongestStreak_When_NovosRecorde")
    void should_AtualizarLongestStreak_When_NovosRecorde() {
        Player player = TestDataFactory.umPlayer();
        player.setCurrentStreak(9);
        player.setLongestStreak(9);
        player.setLastActivityDate(LocalDate.now().minusDays(1));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var resultado = streakService.atualizar(player);

        assertThat(resultado.currentStreak()).isEqualTo(10);
        assertThat(resultado.longestStreak()).isEqualTo(10);
    }

    @Test
    @DisplayName("should_NaoAtualizar_When_LongestStreakJaMaior")
    void should_NaoAtualizar_When_LongestStreakJaMaior() {
        Player player = TestDataFactory.umPlayer();
        player.setCurrentStreak(3);
        player.setLongestStreak(20);
        player.setLastActivityDate(LocalDate.now().minusDays(1));
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var resultado = streakService.atualizar(player);

        assertThat(resultado.longestStreak()).isEqualTo(20);
    }
}
