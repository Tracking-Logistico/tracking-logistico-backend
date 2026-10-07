package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.domain.exception.*;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class PedidoControllerAdvice {

    @ExceptionHandler(PedidoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarPedidoNoEncontrado(PedidoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "No se encontró el envío solicitado"));
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<Map<String, String>> manejarTransicionInvalida(TransicionEstadoInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("estado", ex.getMessage()));
    }

    @ExceptionHandler(TrackingNoActivoException.class)
    public ResponseEntity<Map<String, String>> manejarTrackingNoActivo(TrackingNoActivoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("tracking", ex.getMessage()));
    }

    @ExceptionHandler(EnvioFinalizadoException.class)
    public ResponseEntity<Map<String, String>> manejarEnvioFinalizado(EnvioFinalizadoException ex) {
        return error(HttpStatus.CONFLICT, "ENVIO_FINALIZADO", "Un envío finalizado no puede modificar su estado");
    }

    @ExceptionHandler(CodigoQrInvalidoException.class)
    public ResponseEntity<Map<String, String>> manejarQrInvalido(CodigoQrInvalidoException ex) {
        return error(HttpStatus.BAD_REQUEST, "QR_INVALIDO", ex.getMessage());
    }

    @ExceptionHandler(CodigoQrOtroEnvioException.class)
    public ResponseEntity<Map<String, String>> manejarQrOtroEnvio(CodigoQrOtroEnvioException ex) {
        return error(HttpStatus.CONFLICT, "QR_OTRO_ENVIO", "El código escaneado corresponde a otro envío");
    }

    @ExceptionHandler(EnvioNoAsignadoAlConductorException.class)
    public ResponseEntity<Map<String, String>> manejarNoAsignado(EnvioNoAsignadoAlConductorException ex) {
        return error(HttpStatus.FORBIDDEN, "ENVIO_NO_ASIGNADO", "El envío no está asignado al conductor autenticado");
    }

    @ExceptionHandler(UbicacionInvalidaException.class)
    public ResponseEntity<Map<String, String>> manejarUbicacionInvalida(UbicacionInvalidaException ex) {
        return error(HttpStatus.BAD_REQUEST, "UBICACION_INVALIDA", ex.getMessage());
    }

    @ExceptionHandler(FechaDispositivoInvalidaException.class)
    public ResponseEntity<Map<String, String>> manejarFechaDispositivo(FechaDispositivoInvalidaException ex) {
        return error(HttpStatus.BAD_REQUEST, "FECHA_DISPOSITIVO_INVALIDA", ex.getMessage());
    }

    @ExceptionHandler(TipoIncidenciaInvalidoException.class)
    public ResponseEntity<Map<String, String>> manejarTipoIncidencia(TipoIncidenciaInvalidoException ex) {
        return error(HttpStatus.BAD_REQUEST, "INCIDENCIA_INVALIDA", ex.getMessage());
    }

    @ExceptionHandler(ComentarioIncidenciaRequeridoException.class)
    public ResponseEntity<Map<String, String>> manejarComentarioRequerido(ComentarioIncidenciaRequeridoException ex) {
        return error(HttpStatus.BAD_REQUEST, "COMENTARIO_OBLIGATORIO", ex.getMessage());
    }

    @ExceptionHandler(IncidenciaNoPermitidaException.class)
    public ResponseEntity<Map<String, String>> manejarIncidenciaNoPermitida(IncidenciaNoPermitidaException ex) {
        return error(HttpStatus.CONFLICT, "INCIDENCIA_NO_PERMITIDA", ex.getMessage());
    }

    @ExceptionHandler(FechaReprogramacionInvalidaException.class)
    public ResponseEntity<Map<String, String>> manejarFechaReprogramacion(FechaReprogramacionInvalidaException ex) {
        return error(HttpStatus.BAD_REQUEST, "FECHA_REPROGRAMACION_INVALIDA", ex.getMessage());
    }

    @ExceptionHandler(PlazoVerificacionDireccionVencidoException.class)
    public ResponseEntity<Map<String, String>> manejarPlazoVencido(PlazoVerificacionDireccionVencidoException ex) {
        return error(HttpStatus.CONFLICT, "PLAZO_DIRECCION_VENCIDO", ex.getMessage());
    }

    @ExceptionHandler(ConflictoConcurrenciaException.class)
    public ResponseEntity<Map<String, String>> manejarConflictoConcurrencia(ConflictoConcurrenciaException ex) {
        return error(HttpStatus.CONFLICT, "CONFLICTO_CONCURRENCIA", ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String code, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("code", code, "error", mensaje));
    }
}
