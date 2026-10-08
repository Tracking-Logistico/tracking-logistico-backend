package com.udea.demo.rutas.infrastructure.controller;

import com.udea.demo.rutas.application.dto.DetalleEntregaDTO;
import com.udea.demo.rutas.application.dto.ParadaPanelDTO;
import com.udea.demo.rutas.application.dto.ProgresoPanelDTO;
import com.udea.demo.rutas.application.dto.SiguienteParadaDTO;
import com.udea.demo.rutas.interfaces.services.PanelConductorServiceI;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/conductor/panel")
public class PanelConductorController {

    private final PanelConductorServiceI panel;

    public PanelConductorController(PanelConductorServiceI panel) {
        this.panel = panel;
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
}