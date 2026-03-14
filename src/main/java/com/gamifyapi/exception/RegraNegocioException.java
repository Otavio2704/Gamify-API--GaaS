package com.gamifyapi.exception;

/**
 * Lançada quando uma regra de negócio é violada.
 * Resulta em HTTP 422 (Unprocessable Entity).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
