package com.udea.demo.usuarios.infrastructure.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.udea.demo.usuarios.domain.exception.CodigoEmpleadoRequeridoException;
import com.udea.demo.usuarios.domain.exception.CuentaInactivaException;
import com.udea.demo.usuarios.domain.exception.EmailYaRegistradoException;
import com.udea.demo.usuarios.domain.exception.LicenciaRequeridaException;
import com.udea.demo.usuarios.domain.exception.PasswordDebilException;
import com.udea.demo.usuarios.domain.exception.PasswordNoCoincideException;
import com.udea.demo.usuarios.domain.exception.RolInternoInvalidoException;
import com.udea.demo.usuarios.domain.exception.UsuarioNoEncontradoException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class UsuarioControllerAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidaciones(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errores.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
    }
    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<Map<String, String>> manejarEmailDuplicado(EmailYaRegistradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("email", ex.getMessage()));
    }

    @ExceptionHandler(RolInternoInvalidoException.class)
    public ResponseEntity<Map<String, String>> manejarRolInvalido(RolInternoInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("rol", ex.getMessage()));
    }

    @ExceptionHandler(LicenciaRequeridaException.class)
    public ResponseEntity<Map<String, String>> manejarLicencia(LicenciaRequeridaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("licencia", ex.getMessage()));
    }

    @ExceptionHandler(CodigoEmpleadoRequeridoException.class)
    public ResponseEntity<Map<String, String>> manejarCodigoEmpleado(CodigoEmpleadoRequeridoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("codigoEmpleado", ex.getMessage()));
    }

    @ExceptionHandler(PasswordDebilException.class)
    public ResponseEntity<Map<String, String>> manejarPasswordDebil(PasswordDebilException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("password", ex.getMessage()));
    }

    @ExceptionHandler(PasswordNoCoincideException.class)
    public ResponseEntity<Map<String, String>> manejarPasswordNoCoincide(PasswordNoCoincideException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("confirmarPassword", ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CuentaInactivaException.class)
    public ResponseEntity<Map<String, String>> manejarCuentaInactiva(CuentaInactivaException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }
}