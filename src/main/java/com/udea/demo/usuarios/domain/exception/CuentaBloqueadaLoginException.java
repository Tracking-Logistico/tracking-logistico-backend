package com.udea.demo.usuarios.domain.exception;

import java.time.Duration;
import java.time.LocalDateTime;

public class CuentaBloqueadaLoginException extends RuntimeException {
    public CuentaBloqueadaLoginException(LocalDateTime bloqueadaHasta) {
        super("Cuenta bloqueada temporalmente. Intenta nuevamente en "
                + Math.max(1, Duration.between(LocalDateTime.now(), bloqueadaHasta).toMinutes())
                + " minutos");
    }
}