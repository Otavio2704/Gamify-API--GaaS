package com.gamifyapi.service;

import com.gamifyapi.dto.response.ActionResultResponse.LevelUpDetails;
import com.gamifyapi.entity.LevelConfig;
import com.gamifyapi.entity.Player;
import com.gamifyapi.repository.LevelConfigRepository;
import com.gamifyapi.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Gerencia a progressão de níveis do player.
 * Suporta level up múltiplo (saltando vários níveis de uma vez).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LevelService {

    private final LevelConfigRepository levelConfigRepository;
    private final PlayerRepository playerRepository;

    /**
     * Verifica e aplica level up baseado no XP atual do player.
     * Suporta múltiplos level-ups de uma vez.
     *
     * @return detalhes do level up (ou {@code nenhum} se não ocorreu)
     */
    @Transactional
    public LevelUpDetails verificarLevelUp(Player player) {
        int nivelAnterior = player.getCurrentLevel();
        int novoNivel = calcularNivel(player.getTenantId(), player.getTotalXp());

        if (novoNivel <= nivelAnterior) {
            return LevelUpDetails.nenhum(nivelAnterior);
        }

        player.setCurrentLevel(novoNivel);
        playerRepository.save(player);

        String titulo = getTitulo(player.getTenantId(), novoNivel);

        log.info("Player '{}' subiu do nível {} para {} (XP: {})",
                player.getExternalId(), nivelAnterior, novoNivel, player.getTotalXp());

        return LevelUpDetails.de(nivelAnterior, novoNivel, titulo);
    }

    /**
     * Calcula o nível correspondente ao XP usando a tabela do tenant.
     * Usa tabela padrão (nível N = N*100 XP) se o tenant não configurou.
     */
    public int calcularNivel(Long tenantId, int totalXp) {
        List<LevelConfig> configs = levelConfigRepository
            .findAllByTenantIdOrderByLevelAsc(tenantId);

        if (configs.isEmpty()) {
            return Math.max(1, totalXp / 100 + 1);
        }

        // O nível é o maior cujo xpRequired <= totalXp
        return configs.stream()
            .filter(c -> c.getXpRequired() <= totalXp)
            .max(Comparator.comparingInt(LevelConfig::getLevel))
            .map(LevelConfig::getLevel)
            .orElse(1);
    }

    /**
     * Retorna o título do nível para o tenant. Null se não configurado.
     */
    public String getTitulo(Long tenantId, int nivel) {
        return levelConfigRepository.findAllByTenantIdOrderByLevelAsc(tenantId)
            .stream()
            .filter(c -> c.getLevel().equals(nivel))
            .findFirst()
            .map(LevelConfig::getTitle)
            .orElse(null);
    }

    /**
     * Retorna o XP necessário para o próximo nível. Null se já estiver no máximo.
     */
    public Integer getXpProximoNivel(Long tenantId, int nivelAtual, int totalXp) {
        List<LevelConfig> configs = levelConfigRepository
            .findAllByTenantIdOrderByLevelAsc(tenantId);

        if (configs.isEmpty()) {
            int proximoXp = (nivelAtual) * 100;
            return proximoXp > totalXp ? proximoXp - totalXp : null;
        }

        return configs.stream()
            .filter(c -> c.getLevel() > nivelAtual)
            .min(Comparator.comparingInt(LevelConfig::getLevel))
            .map(c -> c.getXpRequired() - totalXp)
            .orElse(null);
    }
}
