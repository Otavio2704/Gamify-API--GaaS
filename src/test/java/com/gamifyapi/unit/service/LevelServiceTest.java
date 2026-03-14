package com.gamifyapi.unit.service;

import com.gamifyapi.entity.LevelConfig;
import com.gamifyapi.entity.Player;
import com.gamifyapi.repository.LevelConfigRepository;
import com.gamifyapi.repository.PlayerRepository;
import com.gamifyapi.service.LevelService;
import com.gamifyapi.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LevelService — cálculo de nível e XP")
class LevelServiceTest {

    @Mock
    private LevelConfigRepository levelConfigRepository;

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private LevelService levelService;

    @Test
    @DisplayName("should_UsarFormulaDefault_When_SemConfiguracaoNiveis")
    void should_UsarFormulaDefault_When_SemConfiguracaoNiveis() {
        when(levelConfigRepository.findAllByTenantIdOrderByLevelAsc(anyLong()))
            .thenReturn(Collections.emptyList());
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Player player = TestDataFactory.umPlayerComXp(150);
        player.setCurrentLevel(1);

        var resultado = levelService.verificarLevelUp(player);

        // fórmula: 150/100 + 1 = 2 (mas como nivel atual é 1 < 2, sobe)
        assertThat(resultado.happened()).isTrue();
        assertThat(resultado.newLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_SubirParaNivel2_When_XpSuficiente")
    void should_SubirParaNivel2_When_XpSuficiente() {
        List<LevelConfig> configs = List.of(
            TestDataFactory.umNivelConfig(1, 0),
            TestDataFactory.umNivelConfig(2, 100),
            TestDataFactory.umNivelConfig(3, 300)
        );
        when(levelConfigRepository.findAllByTenantIdOrderByLevelAsc(anyLong()))
            .thenReturn(configs);
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Player player = TestDataFactory.umPlayerComXp(150);
        player.setCurrentLevel(1);

        var resultado = levelService.verificarLevelUp(player);

        assertThat(resultado.happened()).isTrue();
        assertThat(resultado.previousLevel()).isEqualTo(1);
        assertThat(resultado.newLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_PularMultiplosNiveis_When_XpGrandeGanho")
    void should_PularMultiplosNiveis_When_XpGrandeGanho() {
        List<LevelConfig> configs = List.of(
            TestDataFactory.umNivelConfig(1, 0),
            TestDataFactory.umNivelConfig(2, 100),
            TestDataFactory.umNivelConfig(3, 300),
            TestDataFactory.umNivelConfig(4, 600)
        );
        when(levelConfigRepository.findAllByTenantIdOrderByLevelAsc(anyLong()))
            .thenReturn(configs);
        when(playerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Player player = TestDataFactory.umPlayerComXp(700);
        player.setCurrentLevel(1);

        var resultado = levelService.verificarLevelUp(player);

        assertThat(resultado.happened()).isTrue();
        assertThat(resultado.newLevel()).isEqualTo(4);
    }

    @Test
    @DisplayName("should_NaoSubirNivel_When_XpInsuficiente")
    void should_NaoSubirNivel_When_XpInsuficiente() {
        List<LevelConfig> configs = List.of(
            TestDataFactory.umNivelConfig(1, 0),
            TestDataFactory.umNivelConfig(2, 100)
        );
        when(levelConfigRepository.findAllByTenantIdOrderByLevelAsc(anyLong()))
            .thenReturn(configs);

        Player player = TestDataFactory.umPlayerComXp(50);
        player.setCurrentLevel(1);

        var resultado = levelService.verificarLevelUp(player);

        assertThat(resultado.happened()).isFalse();
        assertThat(resultado.newLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_RetornarXpFaltante_When_GetXpProximoNivel")
    void should_RetornarXpFaltante_When_GetXpProximoNivel() {
        List<LevelConfig> configs = List.of(
            TestDataFactory.umNivelConfig(1, 0),
            TestDataFactory.umNivelConfig(2, 100),
            TestDataFactory.umNivelConfig(3, 300)
        );
        when(levelConfigRepository.findAllByTenantIdOrderByLevelAsc(anyLong()))
            .thenReturn(configs);

        // player está no nível 1 com 60 XP → faltam 40 para nível 2
        Integer xpFaltante = levelService.getXpProximoNivel(1L, 1, 60);

        assertThat(xpFaltante).isEqualTo(40);
    }
}
