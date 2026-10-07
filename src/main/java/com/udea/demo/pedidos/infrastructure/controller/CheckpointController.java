package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.application.service.NovedadService;
import com.udea.demo.pedidos.application.service.SincronizacionCheckpointService;
import com.udea.demo.pedidos.interfaces.services.CheckpointServiceI;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/pedidos")
public class CheckpointController {
    private final CheckpointServiceI checkpoints;
    private final SincronizacionCheckpointService sincronizacion;
    private final NovedadService novedades;

    public CheckpointController(CheckpointServiceI checkpoints, SincronizacionCheckpointService sincronizacion,
                                NovedadService novedades) {
        this.checkpoints = checkpoints;
        this.sincronizacion = sincronizacion;
        this.novedades = novedades;
    }

    @PostMapping("/{id}/checkpoints")
    public ResponseEntity<CheckpointResponseDTO> registrar(@PathVariable Long id,
                                                           @Valid @RequestBody RegistrarCheckpointRequestDTO dto) {
        RegistroCheckpointResultadoDTO resultado = checkpoints.registrar(id, dto);
        return ResponseEntity.status(resultado.duplicado() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(resultado.checkpoint());
    }

    @PostMapping("/checkpoints/sincronizacion")
    public ResponseEntity<SincronizacionCheckpointsResponseDTO> sincronizar(
            @Valid @RequestBody SincronizarCheckpointsRequestDTO dto) {
        return ResponseEntity.ok(sincronizacion.sincronizar(dto));
    }

    @GetMapping("/checkpoints/mios/pendientes-revision")
    public ResponseEntity<CheckpointsPendientesRevisionDTO> misPendientesRevision() {
        return ResponseEntity.ok(checkpoints.misPendientesRevision());
    }

    @GetMapping("/checkpoints/pendientes-revision")
    public ResponseEntity<Page<CheckpointResponseDTO>> pendientesRevision(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(checkpoints.pendientesRevision(limitar(pageable)));
    }

    @GetMapping("/novedades")
    public ResponseEntity<Page<NovedadResponseDTO>> novedades(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(novedades.listar(limitar(pageable)));
    }

    private Pageable limitar(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), Math.min(Math.max(pageable.getPageSize(), 1), 50));
    }
}
