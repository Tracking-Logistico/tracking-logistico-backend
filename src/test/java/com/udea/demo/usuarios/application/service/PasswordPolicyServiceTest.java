package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.domain.exception.PasswordDebilException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordPolicyService - complejidad de contraseñas")
class PasswordPolicyServiceTest {
    private final PasswordPolicyService politica = new PasswordPolicyService();

    @ParameterizedTest
    @ValueSource(strings = {"corta1!", "solominusculas1!", "SOLOMAYUSCULAS1!", "SinNumerosEspecial!", "SinEspecial123"})
    @DisplayName("Rechaza contraseñas que no cumplen la complejidad mínima")
    void rechazaDebiles(String password) {
        assertThatThrownBy(() -> politica.validar(password, "ana@mail.com", "Ana"))
                .isInstanceOf(PasswordDebilException.class)
                .hasMessageContaining("mínimo 8 caracteres");
    }

    @Test
    @DisplayName("Rechaza una contraseña igual al correo")
    void igualAlCorreo() {
        assertThatThrownBy(() -> politica.validar("Ana@Mail.com1", "ana@mail.com1", "Ana"))
                .isInstanceOf(PasswordDebilException.class);
    }

    @Test
    @DisplayName("Acepta una contraseña robusta")
    void aceptaRobusta() {
        assertThatCode(() -> politica.validar("NuevaPass123!", "ana@mail.com", "Ana")).doesNotThrowAnyException();
    }
}
