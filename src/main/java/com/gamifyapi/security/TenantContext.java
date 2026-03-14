package com.gamifyapi.security;

import com.gamifyapi.entity.Tenant;

/**
 * Contexto de tenant por thread (ThreadLocal).
 * Armazena o tenant autenticado durante o ciclo de vida da requisição.
 * Toda a aplicação consulta esse contexto para saber qual tenant está ativo.
 */
public final class TenantContext {

    private static final ThreadLocal<Tenant> CONTEXT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenant(Tenant tenant) {
        CONTEXT.set(tenant);
    }

    public static Tenant getTenant() {
        return CONTEXT.get();
    }

    public static Long getTenantId() {
        Tenant t = CONTEXT.get();
        return t != null ? t.getId() : null;
    }

    /** Deve ser chamado ao final de cada requisição para evitar vazamento de memória. */
    public static void clear() {
        CONTEXT.remove();
    }
}
