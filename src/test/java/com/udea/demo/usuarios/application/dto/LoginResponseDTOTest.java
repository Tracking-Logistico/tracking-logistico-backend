package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginResponseDTO - estructura e inmutabilidad")
class LoginResponseDTOTest {

    @Test
    @DisplayName("Crea instancia correcta y expone todos sus campos con los valores esperados")
    void crearInstancia_asignaCamposCorrectamente() {
        // Arrange
        String accessToken = "access-token-xyz";
        String refreshToken = "refresh-token-abc";
        LocalDateTime accessExpires = LocalDateTime.of(2026, 9, 19, 12, 30);
        LocalDateTime refreshExpires = LocalDateTime.of(2026, 9, 26, 12, 30);
        Rol rol = Rol.OPERADOR;
        String panel = "/panel/operador";

        // Act
        LoginResponseDTO dto = new LoginResponseDTO(
                accessToken, refreshToken, accessExpires, refreshExpires, rol, panel
        );

        // Assert
        assertThat(dto.accessToken()).isEqualTo(accessToken);
        assertThat(dto.refreshToken()).isEqualTo(refreshToken);
        assertThat(dto.accessTokenExpiresAt()).isEqualTo(accessExpires);
        assertThat(dto.refreshTokenExpiresAt()).isEqualTo(refreshExpires);
        assertThat(dto.rol()).isEqualTo(Rol.OPERADOR);
        assertThat(dto.panel()).isEqualTo("/panel/operador");
    }

    @Test
    @DisplayName("Compara igualdad de instancias con los mismos valores")
    void equalsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime later = now.plusDays(7);

        LoginResponseDTO dto1 = new LoginResponseDTO("tok1", "ref1", now, later, Rol.CLIENTE, "/panel/cliente");
        LoginResponseDTO dto2 = new LoginResponseDTO("tok1", "ref1", now, later, Rol.CLIENTE, "/panel/cliente");
        LoginResponseDTO dto3 = new LoginResponseDTO("tok2", "ref1", now, later, Rol.CLIENTE, "/panel/cliente");

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.toString()).contains("tok1").contains("/panel/cliente");
    }
}
