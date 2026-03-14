package com.gamifyapi.exception;

/**
 * Lançada quando o cooldown de uma ação ainda está ativo para o player.
 * Resulta em HTTP 429 (Too Many Requests).
 */
public class CooldownAtivoException extends RuntimeException {

    /** Segundos restantes até o cooldown expirar. */
    private final long segundosRestantes;

    public CooldownAtivoException(long segundosRestantes) {
        super("Cooldown ativo. Tente novamente em %d segundo(s).".formatted(segundosRestantes));
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}
