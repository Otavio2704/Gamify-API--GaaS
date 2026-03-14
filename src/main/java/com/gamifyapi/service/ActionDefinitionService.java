package com.gamifyapi.service;

import com.gamifyapi.dto.request.ActionDefinitionRequest;
import com.gamifyapi.dto.response.ActionDefinitionResponse;
import com.gamifyapi.entity.ActionDefinition;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.exception.ConflitoException;
import com.gamifyapi.exception.RecursoNaoEncontradoException;
import com.gamifyapi.repository.ActionDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionDefinitionService {

    private final ActionDefinitionRepository actionDefinitionRepository;

    @Transactional
    public ActionDefinitionResponse criar(Tenant tenant, ActionDefinitionRequest request) {
        if (actionDefinitionRepository.existsByTenantIdAndCode(tenant.getId(), request.code())) {
            throw new ConflitoException("Código de ação já existe: " + request.code());
        }

        ActionDefinition acao = ActionDefinition.builder()
                .tenant(tenant)
                .code(request.code())
                .displayName(request.displayName())
                .description(request.description())
                .xpValue(request.xpValue())
                .cooldownSeconds(request.cooldownSeconds())
                .build();

        return toResponse(actionDefinitionRepository.save(acao));
    }

    public List<ActionDefinitionResponse> listar(Long tenantId) {
        return actionDefinitionRepository.findAllByTenantId(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ActionDefinitionResponse atualizar(Long tenantId, Long id, ActionDefinitionRequest request) {
        ActionDefinition acao = buscarPorIdETenant(tenantId, id);
        acao.setDisplayName(request.displayName());
        acao.setDescription(request.description());
        acao.setXpValue(request.xpValue());
        acao.setCooldownSeconds(request.cooldownSeconds());
        return toResponse(actionDefinitionRepository.save(acao));
    }

    @Transactional
    public void remover(Long tenantId, Long id) {
        ActionDefinition acao = buscarPorIdETenant(tenantId, id);
        acao.setActive(false);
        actionDefinitionRepository.save(acao);
    }

    private ActionDefinition buscarPorIdETenant(Long tenantId, Long id) {
        return actionDefinitionRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("ActionDefinition", id));
    }

    private ActionDefinitionResponse toResponse(ActionDefinition a) {
        return new ActionDefinitionResponse(
            a.getId(), a.getCode(), a.getDisplayName(), a.getDescription(),
            a.getXpValue(), a.getCooldownSeconds(), a.getActive(), a.getCreatedAt()
        );
    }
}
