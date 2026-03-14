package com.gamifyapi.security;

import com.gamifyapi.entity.Tenant;
import com.gamifyapi.exception.AutenticacaoException;

/**
 * Utilitários para acessar o tenant autenticado no contexto da requisição.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Retorna o ID do tenant autenticado na requisição atual.
     *
     * @throws AutenticacaoException se não houver tenant no contexto
     */
    public static Long getTenantIdAtual() {
        Long id = TenantContext.getTenantId();
        if (id == null) {
            throw new AutenticacaoException("Tenant não autenticado");
        }
        return id;
    }

    /**
     * Retorna a entidade Tenant autenticada na requisição atual.
     *
     * @throws AutenticacaoException se não houver tenant no contexto
     */
    public static Tenant getTenantAtual() {
        Tenant tenant = TenantContext.getTenant();
        if (tenant == null) {
            throw new AutenticacaoException("Tenant não autenticado");
        }
        return tenant;
    }
}
