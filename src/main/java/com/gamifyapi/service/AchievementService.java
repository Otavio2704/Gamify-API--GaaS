package com.gamifyapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamifyapi.dto.request.AchievementRequest;
import com.gamifyapi.dto.response.AchievementResponse;
import com.gamifyapi.entity.Achievement;
import com.gamifyapi.dto.response.PlayerAchievementsResponse;
import com.gamifyapi.dto.response.PlayerAchievementsResponse.LockedAchievement;
import com.gamifyapi.dto.response.PlayerAchievementsResponse.UnlockedAchievement;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.repository.PlayerAchievementRepository;
import com.gamifyapi.exception.ConflitoException;
import com.gamifyapi.exception.RecursoNaoEncontradoException;
import com.gamifyapi.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AchievementResponse criar(Tenant tenant, AchievementRequest request) {
        if (achievementRepository.existsByTenantIdAndCode(tenant.getId(), request.code())) {
            throw new ConflitoException("Código de conquista já existe: " + request.code());
        }

        Achievement achievement = Achievement.builder()
                .tenant(tenant)
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .badgeImageUrl(request.badgeImageUrl())
                .xpReward(request.xpReward())
                .criteriaType(request.criteriaType())
                .criteriaValue(toJson(request.criteriaValue()))
                .build();

        return toResponse(achievementRepository.save(achievement));
    }

    public List<AchievementResponse> listar(Long tenantId) {
        return achievementRepository.findAllByTenantId(tenantId)
            .stream().map(this::toResponse).toList();
    }

    /**
     * Retorna conquistas desbloqueadas e bloqueadas de um player.
     */
    @Transactional(readOnly = true)
    public PlayerAchievementsResponse obterConquistasDoPlayer(Long tenantId, Long playerId) {
        var paList = playerAchievementRepository.findAllByPlayerId(playerId);
        Set<Long> desbloqueadasIds = paList.stream()
            .map(pa -> pa.getAchievement().getId()).collect(Collectors.toSet());

        List<UnlockedAchievement> desbloqueadas = paList.stream()
            .map(pa -> new UnlockedAchievement(
                pa.getAchievement().getCode(),
                pa.getAchievement().getName(),
                pa.getAchievement().getDescription(),
                pa.getAchievement().getBadgeImageUrl(),
                pa.getUnlockedAt()
            )).toList();

        List<LockedAchievement> bloqueadas = achievementRepository.findAllByTenantId(tenantId)
            .stream()
            .filter(a -> !desbloqueadasIds.contains(a.getId()))
            .map(a -> new LockedAchievement(
                a.getCode(), a.getName(), a.getDescription(), a.getBadgeImageUrl(), null))
            .toList();

        return new PlayerAchievementsResponse(desbloqueadas, bloqueadas);
    }

    @Transactional
    public AchievementResponse atualizar(Long tenantId, Long id, AchievementRequest request) {
        Achievement a = buscarPorIdETenant(tenantId, id);
        a.setName(request.name());
        a.setDescription(request.description());
        a.setBadgeImageUrl(request.badgeImageUrl());
        a.setXpReward(request.xpReward());
        a.setCriteriaType(request.criteriaType());
        a.setCriteriaValue(toJson(request.criteriaValue()));
        return toResponse(achievementRepository.save(a));
    }

    @Transactional
    public void remover(Long tenantId, Long id) {
        Achievement a = buscarPorIdETenant(tenantId, id);
        a.setActive(false);
        achievementRepository.save(a);
    }

    private Achievement buscarPorIdETenant(Long tenantId, Long id) {
        return achievementRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Achievement", id));
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    private AchievementResponse toResponse(Achievement a) {
        return new AchievementResponse(
            a.getId(), a.getCode(), a.getName(), a.getDescription(),
            a.getBadgeImageUrl(), a.getXpReward(), a.getCriteriaType(),
            a.getCriteriaValueAsMap(), a.getActive(), a.getCreatedAt()
        );
    }
}
