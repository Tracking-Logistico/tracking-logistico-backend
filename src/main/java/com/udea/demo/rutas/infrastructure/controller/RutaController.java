package com.udea.demo.rutas.infrastructure.controller;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.rutas.application.dto.AsignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReasignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReordenarRutaRequestDTO;
import com.udea.demo.rutas.application.dto.RutaResponseDTO;
import com.udea.demo.rutas.interfaces.services.RutaServiceI;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rutas")
public class RutaController {

    private final RutaServiceI rutaService;

    public RutaController(RutaServiceI rutaService) {
        this.rutaService = rutaService;
    }

    // Criterio 1: Visualización de envíos pendientes de asignación
    @GetMapping("/envios-pendientes")
    public ResponseEntity<List<PedidoResponseDTO>> listarEnviosPendientes() {
        return ResponseEntity.ok(rutaService.listarEnviosPendientesDeAsignacion());
    }

    // Criterio 2: Asignación de envíos a un conductor
    @PostMapping("/asignaciones")
    public ResponseEntity<RutaResponseDTO> asignarEnvio(@Valid @RequestBody AsignarEnvioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rutaService.asignarEnvio(dto));
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
