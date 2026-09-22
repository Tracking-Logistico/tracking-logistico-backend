package com.udea.demo.usuarios.application.service;


import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class PasswordTemporalService {

    private static final String MAYUS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MINUS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITOS = "0123456789";
    private static final String ESPECIALES = "@#$%^&+=!._-";
    private static final String TODOS = MAYUS + MINUS + DIGITOS + ESPECIALES;

    private final SecureRandom random = new SecureRandom();

    public String generar() {
        StringBuilder sb = new StringBuilder(16);

        sb.append(MAYUS.charAt(random.nextInt(MAYUS.length())));
        sb.append(MINUS.charAt(random.nextInt(MINUS.length())));
        sb.append(DIGITOS.charAt(random.nextInt(DIGITOS.length())));
        sb.append(ESPECIALES.charAt(random.nextInt(ESPECIALES.length())));

        for (int i = 4; i < 16; i++) {
            sb.append(TODOS.charAt(random.nextInt(TODOS.length())));
        }

        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
