package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.application.service.ReprogramacionEntregaService;
import com.udea.demo.pedidos.application.service.VerificacionDireccionService;
import com.udea.demo.pedidos.interfaces.services.IncidenciaServiceI;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/pedidos")
public class IncidenciaController {
    private final IncidenciaServiceI incidencias;
    private final ReprogramacionEntregaService reprogramacion;
    private final VerificacionDireccionService verificacionDireccion;

    public IncidenciaController(IncidenciaServiceI incidencias, ReprogramacionEntregaService reprogramacion,
                                VerificacionDireccionService verificacionDireccion) {
        this.incidencias = incidencias;
        this.reprogramacion = reprogramacion;
        this.verificacionDireccion = verificacionDireccion;
    }

    @GetMapping("/incidencias/tipos")
    public ResponseEntity<List<TipoIncidenciaResponseDTO>> catalogo() {
        return ResponseEntity.ok(incidencias.catalogo());
    }

    @PostMapping("/{id}/incidencias")
    public ResponseEntity<RegistroIncidenciaResponseDTO> registrar(@PathVariable Long id,
                                                                   @Valid @RequestBody RegistrarIncidenciaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidencias.registrar(id, dto));
    }

    @GetMapping("/{id}/incidencias")
    public ResponseEntity<IncidenciasPedidoResponseDTO> listar(@PathVariable Long id) {
        return ResponseEntity.ok(incidencias.listar(id));
    }

    @GetMapping("/mios/tracking/{numeroTracking}/reprogramacion/rango")
    public ResponseEntity<RangoReprogramacionResponseDTO> rangoReprogramacion(@PathVariable String numeroTracking) {
        return ResponseEntity.ok(reprogramacion.rangoDisponible(numeroTracking));
    }

    @PostMapping("/mios/tracking/{numeroTracking}/reprogramacion")
    public ResponseEntity<SeguimientoClienteResponseDTO> reprogramar(@PathVariable String numeroTracking,
                                                                     @Valid @RequestBody ReprogramarEntregaRequestDTO dto) {
        return ResponseEntity.ok(reprogramacion.reprogramar(numeroTracking, dto));
    }

    @PutMapping("/mios/tracking/{numeroTracking}/direccion")
    public ResponseEntity<SeguimientoClienteResponseDTO> confirmarDireccion(@PathVariable String numeroTracking,
                                                                            @Valid @RequestBody ConfirmarDireccionRequestDTO dto) {
        return ResponseEntity.ok(verificacionDireccion.confirmar(numeroTracking, dto));
    }
}
