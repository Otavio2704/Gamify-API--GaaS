package com.gamifyapi.service;

import com.gamifyapi.entity.Player;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Gerencia criação e atualização de players.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository playerRepository;

    /**
     * Busca o player pelo externalId + tenantId ou cria um novo se não existir.
     * Atualiza o displayName se foi fornecido e é diferente do atual.
     */
    @Transactional
    public Player buscarOuCriar(Tenant tenant, String externalId, String displayName) {
        return playerRepository
            .findByTenant_IdAndExternalId(tenant.getId(), externalId)
            .map(player -> {
                if (StringUtils.hasText(displayName)
                        && !displayName.equals(player.getDisplayName())) {
                    player.setDisplayName(displayName);
                    playerRepository.save(player);
                }
                return player;
            })
            .orElseGet(() -> {
                log.info("Criando novo player '{}' para tenant '{}'",
                        externalId, tenant.getId());
                Player novo = Player.builder()
                        .tenant(tenant)
                        .externalId(externalId)
                        .displayName(displayName)
                        .totalXp(0)
                        .currentLevel(1)
                        .currentStreak(0)
                        .longestStreak(0)
                        .build();
                return playerRepository.save(novo);
            });
    }

    /**
     * Retorna o ID do tenant de um player a partir do objeto Player.
     * Convenência para evitar lazy load desnecessário.
     */
    public Long getTenantId(Player player) {
        return player.getTenant().getId();
    }
}
