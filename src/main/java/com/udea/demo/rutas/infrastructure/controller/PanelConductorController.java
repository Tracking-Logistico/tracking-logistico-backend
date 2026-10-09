package com.udea.demo.rutas.infrastructure.controller;

import com.udea.demo.rutas.application.dto.DetalleEntregaDTO;
import com.udea.demo.rutas.application.dto.ParadaPanelDTO;
import com.udea.demo.rutas.application.dto.ProgresoPanelDTO;
import com.udea.demo.rutas.application.dto.SiguienteParadaDTO;
import com.udea.demo.rutas.application.dto.RutaConductorResponseDTO;
import com.udea.demo.rutas.interfaces.services.PanelConductorServiceI;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.application.service.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/conductor/panel")
public class PanelConductorController {

    private final PanelConductorServiceI panel;
    private final ResultadoEntregaServiceI resultados;
    private final SincronizacionEntregasService sincronizacion;

    public PanelConductorController(PanelConductorServiceI panel, ResultadoEntregaServiceI resultados,
                                    SincronizacionEntregasService sincronizacion) {
        this.panel = panel; this.resultados = resultados; this.sincronizacion = sincronizacion;
    }

    @GetMapping("/progreso")
    public ResponseEntity<ProgresoPanelDTO> progreso() {
        return ResponseEntity.ok(panel.progreso());
    }

    @GetMapping("/entregas")
    public ResponseEntity<List<ParadaPanelDTO>> entregas() {
        return ResponseEntity.ok(panel.entregasAsignadas());
    }

    @GetMapping("/entregas/{pedidoId}")
    public ResponseEntity<DetalleEntregaDTO> detalle(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(panel.detalleEntrega(pedidoId));
    }

    @GetMapping("/siguiente")
    public ResponseEntity<SiguienteParadaDTO> siguiente() {
        return ResponseEntity.of(java.util.Optional.ofNullable(panel.siguienteParada()));
    }

    @GetMapping("/ruta")
    public ResponseEntity<RutaConductorResponseDTO> ruta() {
        return ResponseEntity.ok(panel.ruta());
    }

    @GetMapping("/catalogo-novedades")
    public ResponseEntity<java.util.List<NovedadEntregaResponseDTO>> catalogo() {
        return ResponseEntity.ok(resultados.catalogo());
    }

    @PostMapping("/entregas/{pedidoId}/resultado")
    public ResponseEntity<ResultadoEntregaResponseDTO> resultado(
            @PathVariable Long pedidoId, @Valid @RequestBody ResultadoEntregaRequestDTO dto) {
        ResultadoEntregaResponseDTO response = resultados.registrar(pedidoId, dto);
        return ResponseEntity.status(response.duplicado() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @PostMapping("/entregas/sincronizacion")
    public ResponseEntity<SincronizacionEntregasResponseDTO> sincronizar(
            @Valid @RequestBody SincronizarEntregasRequestDTO dto) {
        return ResponseEntity.ok(sincronizacion.sincronizar(dto));
    }
}