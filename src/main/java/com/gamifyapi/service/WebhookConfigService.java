package com.gamifyapi.service;

import com.gamifyapi.dto.request.WebhookConfigRequest;
import com.gamifyapi.dto.response.WebhookConfigResponse;
import com.gamifyapi.entity.Tenant;
import com.gamifyapi.entity.WebhookConfig;
import com.gamifyapi.exception.RecursoNaoEncontradoException;
import com.gamifyapi.repository.WebhookConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookConfigService {

    private final WebhookConfigRepository webhookConfigRepository;

    @Transactional
    public WebhookConfigResponse criar(Tenant tenant, WebhookConfigRequest request) {
        WebhookConfig wh = WebhookConfig.builder()
                .tenant(tenant)
                .url(request.url())
                .eventType(request.eventType())
                .secretKey(request.secretKey())
                .build();
        return toResponse(webhookConfigRepository.save(wh));
    }

    public List<WebhookConfigResponse> listar(Long tenantId) {
        return webhookConfigRepository.findAllByTenantId(tenantId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public WebhookConfigResponse atualizar(Long tenantId, Long id, WebhookConfigRequest request) {
        WebhookConfig wh = buscarPorIdETenant(tenantId, id);
        wh.setUrl(request.url());
        wh.setEventType(request.eventType());
        wh.setSecretKey(request.secretKey());
        return toResponse(webhookConfigRepository.save(wh));
    }

    @Transactional
    public void remover(Long tenantId, Long id) {
        WebhookConfig wh = buscarPorIdETenant(tenantId, id);
        webhookConfigRepository.delete(wh);
    }

    private WebhookConfig buscarPorIdETenant(Long tenantId, Long id) {
        return webhookConfigRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("WebhookConfig", id));
    }

    private WebhookConfigResponse toResponse(WebhookConfig w) {
        return new WebhookConfigResponse(
            w.getId(), w.getUrl(), w.getEventType(), w.getActive(), w.getCreatedAt()
        );
    }
}
