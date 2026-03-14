package com.gamifyapi.service;

import com.gamifyapi.dto.request.LevelConfigRequest;
import com.gamifyapi.dto.request.LevelConfigRequest.LevelEntry;
import com.gamifyapi.entity.LevelConfig;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.exception.RegraNegocioException;
import com.gamifyapi.repository.LevelConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LevelConfigService {

    private final LevelConfigRepository levelConfigRepository;

    /**
     * Substitui todos os níveis do tenant (POST e PUT têm o mesmo comportamento).
     */
    @Transactional
    public List<LevelConfig> salvar(Tenant tenant, LevelConfigRequest request) {
        // Validação: nível 1 deve ter xpRequired = 0
        request.levels().stream()
            .filter(l -> l.level() == 1 && l.xpRequired() != 0)
            .findAny()
            .ifPresent(l -> {
                throw new RegraNegocioException("Nível 1 deve ter xpRequired = 0");
            });

        // Deleta os antigos e insere os novos
        levelConfigRepository.deleteAllByTenantId(tenant.getId());

        List<LevelConfig> configs = request.levels().stream()
            .map(entry -> LevelConfig.builder()
                .tenant(tenant)
                .level(entry.level())
                .xpRequired(entry.xpRequired())
                .title(entry.title())
                .build())
            .toList();

        return levelConfigRepository.saveAll(configs);
    }

    public List<LevelConfig> listar(Long tenantId) {
        return levelConfigRepository.findAllByTenantIdOrderByLevelAsc(tenantId);
    }
}
