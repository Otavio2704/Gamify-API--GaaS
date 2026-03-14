package com.gamifyapi.service;

import com.gamifyapi.entity.ActionDefinition;
import com.gamifyapi.entity.ActionLog;
import com.gamifyapi.entity.Player;
import com.gamifyapi.repository.ActionLogRepository;
import com.gamifyapi.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Responsável por conceder XP ao player e registrar o ActionLog.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class XpService {

    private final PlayerRepository playerRepository;
    private final ActionLogRepository actionLogRepository;

    /**
     * Concede XP da ação ao player, persiste e registra no log.
     *
     * @return quantidade de XP concedida
     */
    @Transactional
    public int concederXp(Player player, ActionDefinition acao) {
        int xp = acao.getXpValue();
        player.adicionarXp(xp);
        playerRepository.save(player);

        ActionLog log = ActionLog.builder()
                .player(player)
                .actionDefinition(acao)
                .xpGranted(xp)
                .timestamp(Instant.now())
                .build();
        actionLogRepository.save(log);

        log().debug("Concedidos {} XP ao player '{}'. Total: {}",
                xp, player.getExternalId(), player.getTotalXp());
        return xp;
    }

    /**
     * Concede XP bônus (conquista) sem registrar ActionLog separado.
     *
     * @return nova quantidade total de XP
     */
    @Transactional
    public int concederXpBonus(Player player, int xpBonus) {
        player.adicionarXp(xpBonus);
        playerRepository.save(player);
        log().debug("Bônus de {} XP (conquista) ao player '{}'. Total: {}",
                xpBonus, player.getExternalId(), player.getTotalXp());
        return player.getTotalXp();
    }

    private org.slf4j.Logger log() {
        return org.slf4j.LoggerFactory.getLogger(XpService.class);
    }
}
