package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.domain.exception.PasswordDebilException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PasswordPolicyService {
    private static final Pattern COMPLEJIDAD = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$");
    private static final Set<String> COMUNES = Set.of(
            "12345678", "password", "password1", "qwerty123", "abc12345", "admin123", "11111111", "123456789");

    public void validar(String password, String email, String nombre) {
        if (password == null || !COMPLEJIDAD.matcher(password).matches()) {
            throw new PasswordDebilException("La contraseña debe tener mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial");
        }
        String normalizada = password.toLowerCase(Locale.ROOT);
        if (COMUNES.contains(normalizada)) {
            throw new PasswordDebilException("La contraseña es demasiado común");
        }
        if (email != null && normalizada.equals(email.toLowerCase(Locale.ROOT))) {
            throw new PasswordDebilException("La contraseña no puede ser igual al correo");
        }
        if (nombre != null && normalizada.equals(nombre.trim().toLowerCase(Locale.ROOT))) {
            throw new PasswordDebilException("La contraseña no puede ser igual al nombre");
        }
    }
}
