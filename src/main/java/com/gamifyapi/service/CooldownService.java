package com.gamifyapi.service;

import com.gamifyapi.entity.ActionDefinition;
import com.gamifyapi.entity.Player;
import com.gamifyapi.exception.CooldownAtivoException;
import com.gamifyapi.repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Verifica e aplica cooldown entre execuções de uma ação por player.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CooldownService {

    private final ActionLogRepository actionLogRepository;

    /**
     * Valida se o player pode executar a ação agora.
     *
     * @throws CooldownAtivoException se ainda estiver em cooldown
     */
    public void validar(Player player, ActionDefinition acao) {
        int cooldown = acao.getCooldownSeconds();
        if (cooldown <= 0) {
            return; // Sem cooldown
        }

        actionLogRepository
            .findTopByPlayerIdAndActionDefinitionIdOrderByTimestampDesc(
                player.getId(), acao.getId())
            .ifPresent(ultimoLog -> {
                long segundosDecorridos = ChronoUnit.SECONDS.between(
                    ultimoLog.getTimestamp(), Instant.now());
                long segundosRestantes = cooldown - segundosDecorridos;

                if (segundosRestantes > 0) {
                    log.debug("Cooldown ativo para player '{}' na ação '{}': {}s restantes",
                            player.getExternalId(), acao.getCode(), segundosRestantes);
                    throw new CooldownAtivoException(segundosRestantes);
                }
            });
    }
}
