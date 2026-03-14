package com.gamifyapi.service;

import com.gamifyapi.dto.response.ActionResultResponse.StreakInfo;
import com.gamifyapi.entity.Player;
import com.gamifyapi.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Gerencia o cálculo e atualização de streak do player.
 * Baseado em dias do calendário (LocalDate), não em 24h.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private final PlayerRepository playerRepository;

    /**
     * Atualiza o streak do player conforme as regras de negócio.
     *
     * @return informações de streak após processamento
     */
    @Transactional
    public StreakInfo atualizar(Player player) {
        LocalDate hoje = LocalDate.now();
        LocalDate ontem = hoje.minusDays(1);
        LocalDate ultimaAtividade = player.getLastActivityDate();

        boolean resetou = false;

        if (ultimaAtividade == null || ultimaAtividade.isBefore(ontem)) {
            // Primeiro acesso ou pulou um dia — reset
            if (player.getCurrentStreak() > player.getLongestStreak()) {
                player.setLongestStreak(player.getCurrentStreak());
            }
            player.setCurrentStreak(1);
            resetou = ultimaAtividade != null;
            log.debug("Streak resetado para player '{}'", player.getExternalId());

        } else if (ultimaAtividade.equals(ontem)) {
            // Jogou ontem — incrementa
            player.setCurrentStreak(player.getCurrentStreak() + 1);
            if (player.getCurrentStreak() > player.getLongestStreak()) {
                player.setLongestStreak(player.getCurrentStreak());
            }
            log.debug("Streak incrementado para {} no player '{}'",
                    player.getCurrentStreak(), player.getExternalId());

        }
        // Se ultimaAtividade == hoje: não faz nada (já contabilizado)

        player.setLastActivityDate(hoje);
        playerRepository.save(player);

        return new StreakInfo(
            player.getCurrentStreak(),
            player.getLongestStreak(),
            resetou
        );
    }
}
