package com.gamifyapi.exception;

/**
 * Lançada quando há conflito de duplicidade (ex: e-mail ou código já cadastrado).
 * Resulta em HTTP 409.
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
