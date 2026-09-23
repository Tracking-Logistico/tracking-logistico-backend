package com.udea.demo.rutas.infrastructure.controller;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.rutas.application.dto.AsignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.AsignacionMasivaRequestDTO;
import com.udea.demo.rutas.application.dto.NotificacionRutaDTO;
import com.udea.demo.rutas.application.dto.HistorialAsignacionDTO;
import com.udea.demo.rutas.application.service.RegistroAsignacionService;
import com.udea.demo.rutas.application.dto.ReasignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReordenarRutaRequestDTO;
import com.udea.demo.rutas.application.dto.RutaResponseDTO;
import com.udea.demo.rutas.application.dto.ConductorDisponibleDTO;
import com.udea.demo.rutas.interfaces.services.RutaServiceI;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/rutas")
public class RutaController {

    private final RutaServiceI rutaService;
    private final ActorAuthorizationService actorAuthorizationService;
    private final RegistroAsignacionService registro;

    public RutaController(RutaServiceI rutaService, ActorAuthorizationService actorAuthorizationService, RegistroAsignacionService registro) {
        this.rutaService = rutaService;
        this.actorAuthorizationService = actorAuthorizationService;
        this.registro = registro;
    }

    // Criterio 1: Visualización de envíos pendientes de asignación
    @GetMapping("/envios-pendientes")
    public ResponseEntity<List<PedidoResponseDTO>> listarEnviosPendientes() {
        return ResponseEntity.ok(rutaService.listarEnviosPendientesDeAsignacion());
    }

    @GetMapping("/conductores-disponibles")
    public ResponseEntity<List<ConductorDisponibleDTO>> listarConductoresDisponibles() {
        return ResponseEntity.ok(rutaService.listarConductoresDisponibles());
    }

    // Criterio 2: Asignación de envíos a un conductor
    @PostMapping("/asignaciones")
    public ResponseEntity<RutaResponseDTO> asignarEnvio(@Valid @RequestBody AsignarEnvioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rutaService.asignarEnvio(dto));
    }

    @PostMapping("/asignaciones/lote")
    public ResponseEntity<RutaResponseDTO> asignarVarios(@Valid @RequestBody AsignacionMasivaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rutaService.asignarVarios(dto));
    }

    @GetMapping("/mi-ruta")
    public ResponseEntity<RutaResponseDTO> miRuta() {
        Long usuarioId = actorAuthorizationService.actorActual().getId();
        actorAuthorizationService.conductorActualId();
        return ResponseEntity.ok(rutaService.obtenerRutaActivaDeConductor(usuarioId));
    }

    @GetMapping("/mis-notificaciones")
    public ResponseEntity<List<NotificacionRutaDTO>> misNotificaciones() {
        return ResponseEntity.ok(registro.misNotificaciones());
    }

    @PatchMapping("/mis-notificaciones/{id}/leer")
    public ResponseEntity<Void> leer(@PathVariable Long id) {
        registro.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/asignaciones/{pedidoId}/historial")
    public ResponseEntity<List<HistorialAsignacionDTO>> historialAsignaciones(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(registro.historial(pedidoId));
    }

    // Consulta de apoyo para revisar la ruta antes de organizarla
    @GetMapping("/conductores/{conductorId}")
    public ResponseEntity<RutaResponseDTO> obtenerRutaDeConductor(@PathVariable Long conductorId) {
        return ResponseEntity.ok(rutaService.obtenerRutaActivaDeConductor(conductorId));
    }

    // Criterio 3: Organización de la ruta
    @PutMapping("/{rutaId}/orden")
    public ResponseEntity<RutaResponseDTO> reordenarRuta(
            @PathVariable Long rutaId,
            @Valid @RequestBody ReordenarRutaRequestDTO dto) {
        return ResponseEntity.ok(rutaService.reordenarRuta(rutaId, dto));
    }

    // Criterio 4: Reasignación de envíos
    @PutMapping("/asignaciones/reasignar")
    public ResponseEntity<RutaResponseDTO> reasignarEnvio(@Valid @RequestBody ReasignarEnvioRequestDTO dto) {
        return ResponseEntity.ok(rutaService.reasignarEnvio(dto));
    }
}
