package com.gamifyapi.exception;

/**
 * Lançada quando as credenciais de autenticação são inválidas.
 * Resulta em HTTP 401.
 */
public class AutenticacaoException extends RuntimeException {

    public AutenticacaoException(String mensagem) {
        super(mensagem);
    }
}
