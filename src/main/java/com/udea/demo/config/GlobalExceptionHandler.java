package com.udea.demo.config;

import com.udea.demo.usuarios.domain.exception.EntregaCorreoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.LinkedHashMap;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntregaCorreoException.class)
    public ResponseEntity<Map<String, String>> mailUnavailable(EntregaCorreoException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> fields(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("error", "Revisa los campos señalados");
        ex.getBindingResult().getFieldErrors().forEach(field ->
                errors.putIfAbsent(field.getField(), field.getDefaultMessage() == null
                        ? "Campo inválido" : field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "No tienes permiso para realizar esta acción"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> invalidInput(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "La solicitud contiene datos inválidos"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidBusinessInput(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage() == null ? "Solicitud inválida" : ex.getMessage()));
    }

    @ExceptionHandler(com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> orderMissing(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException.class,
            com.udea.demo.pedidos.domain.exception.TrackingNoActivoException.class})
    public ResponseEntity<Map<String, String>> orderConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> invalidState(IllegalStateException ex) {
        if (ex.getCause() != null) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "No fue posible generar el archivo. Puedes reintentar sin cambiar el envío."));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.PessimisticLockingFailureException.class})
    public ResponseEntity<Map<String, String>> concurrentUpdate(RuntimeException ex) {
        log.warn("Operación simultánea detectada: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El envío o la ruta cambió durante la operación. Actualiza y vuelve a intentar."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integrityError(DataIntegrityViolationException ex) {
        log.warn("Operación rechazada por una restricción de base de datos");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El registro entra en conflicto con otro dato existente"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> statusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", "No fue posible completar la solicitud"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> resourceMissing(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Recurso no encontrado"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception ex) {
        log.error("Error no controlado en la API", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "No fue posible completar la operación"));
    }
}
