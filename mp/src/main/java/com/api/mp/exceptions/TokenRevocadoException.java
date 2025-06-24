package com.api.mp.exceptions;

public class TokenRevocadoException extends RuntimeException {
    public TokenRevocadoException(String mensaje) {
        super(mensaje);
    }
}
