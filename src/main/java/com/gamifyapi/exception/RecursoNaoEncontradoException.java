package com.gamifyapi.exception;

/**
 * Lançada quando um recurso não é encontrado no banco de dados.
 * Resulta em HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super("%s não encontrado(a) com id: %s".formatted(recurso, id));
    }
}
