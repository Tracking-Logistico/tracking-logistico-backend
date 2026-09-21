package com.udea.demo.usuarios.infrastructure.controller;

import com.udea.demo.usuarios.domain.exception.CodigoEmpleadoRequeridoException;
import com.udea.demo.usuarios.domain.exception.CredencialesInvalidasException;
import com.udea.demo.usuarios.domain.exception.CuentaBloqueadaLoginException;
import com.udea.demo.usuarios.domain.exception.CuentaInactivaException;
import com.udea.demo.usuarios.domain.exception.EmailYaRegistradoException;
import com.udea.demo.usuarios.domain.exception.LicenciaRequeridaException;
import com.udea.demo.usuarios.domain.exception.PasswordDebilException;
import com.udea.demo.usuarios.domain.exception.PasswordNoCoincideException;
import com.udea.demo.usuarios.domain.exception.RolInternoInvalidoException;
import com.udea.demo.usuarios.domain.exception.SesionInvalidaException;
import com.udea.demo.usuarios.domain.exception.TokenRestablecimientoInvalidoException;
import com.udea.demo.usuarios.domain.exception.UsuarioNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UsuarioControllerAdvice - mapeo HTTP HU-01A / HU-01B")
class UsuarioControllerAdviceTest {

    private final UsuarioControllerAdvice advice = new UsuarioControllerAdvice();

    @Test
    @DisplayName("IllegalArgumentException de cliente duplicado → 400 con clave error")
    void correoDuplicadoCliente_400() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarIllegalArgument(new IllegalArgumentException("El correo ya se encuentra registrado"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("error", "El correo ya se encuentra registrado");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 con errores por campo")
    void validacionPorCampo_400() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "registro");
        binding.addError(new FieldError("registro", "email", "El formato de email no es válido"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<Map<String, String>> respuesta = advice.manejarValidaciones(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("email", "El formato de email no es válido");
    }

    @Test
    @DisplayName("EmailYaRegistradoException → 409")
    void emailYaRegistrado_409() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarEmailDuplicado(new EmailYaRegistradoException("op@tracking.com"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).containsKey("email");
        assertThat(respuesta.getBody().get("email")).contains("op@tracking.com");
    }

    @Test
    @DisplayName("RolInternoInvalidoException → 400 en campo rol")
    void rolInvalido_400() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarRolInvalido(new RolInternoInvalidoException());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsKey("rol");
    }

    @Test
    @DisplayName("LicenciaRequeridaException → 400")
    void licencia_400() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarLicencia(new LicenciaRequeridaException());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsKey("licencia");
    }

    @Test
    @DisplayName("CodigoEmpleadoRequeridoException → 400")
    void codigoEmpleado_400() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarCodigoEmpleado(new CodigoEmpleadoRequeridoException());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsKey("codigoEmpleado");
    }

    @Test
    @DisplayName("PasswordDebilException → 400")
    void passwordDebil_400() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarPasswordDebil(new PasswordDebilException("débil"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("password", "débil");
    }

    @Test
    @DisplayName("PasswordNoCoincideException → 400 en confirmarPassword")
    void passwordNoCoincide_400() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarPasswordNoCoincide(new PasswordNoCoincideException());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("confirmarPassword", "Las contraseñas no coinciden");
    }

    @Test
    @DisplayName("UsuarioNoEncontradoException → 404")
    void usuarioNoEncontrado_404() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarUsuarioNoEncontrado(new UsuarioNoEncontradoException(8L));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody().get("error")).contains("8");
    }

    @Test
    @DisplayName("CuentaInactivaException → 403")
    void cuentaInactiva_403() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarCuentaInactiva(new CuentaInactivaException());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("CredencialesInvalidasException → 401")
    void credenciales_401() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarCredencialesInvalidas(new CredencialesInvalidasException());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).containsEntry("error", "Usuario o contraseña incorrectos");
    }

    @Test
    @DisplayName("CuentaBloqueadaLoginException → 423 Locked")
    void cuentaBloqueada_423() {
        ResponseEntity<Map<String, String>> respuesta =
                advice.manejarCuentaBloqueada(
                        new CuentaBloqueadaLoginException(LocalDateTime.now().plusMinutes(10)));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(respuesta.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("SesionInvalidaException y TokenRestablecimientoInvalidoException → 401")
    void tokensInvalidos_401() {
        ResponseEntity<Map<String, String>> sesion =
                advice.manejarTokenInvalido(new SesionInvalidaException());
        ResponseEntity<Map<String, String>> reset =
                advice.manejarTokenInvalido(new TokenRestablecimientoInvalidoException());

        assertThat(sesion.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(sesion.getBody()).containsKey("error");
        assertThat(reset.getBody()).containsKey("error");
    }
}
