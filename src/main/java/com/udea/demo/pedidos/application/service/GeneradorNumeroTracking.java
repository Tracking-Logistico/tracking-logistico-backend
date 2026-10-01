package com.udea.demo.pedidos.application.service;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class GeneradorNumeroTracking {
    private static final char[] ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();

    public String generar() {
        StringBuilder tracking = new StringBuilder("LT");
        while (tracking.length() < 12) tracking.append(ALFABETO[random.nextInt(ALFABETO.length)]);
        return tracking.toString();
    }
}
