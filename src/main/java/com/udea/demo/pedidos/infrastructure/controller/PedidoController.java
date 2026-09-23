package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/pedidos")
public class PedidoController {
    private final PedidoServiceI pedidoService;

    public PedidoController(PedidoServiceI pedidoService) { this.pedidoService = pedidoService; }

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> recibirPedido(@Valid @RequestBody RecibirPedidoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.recibir(dto));
    }

    @PutMapping("/{id}/corregir")
    public ResponseEntity<PedidoResponseDTO> corregir(@PathVariable Long id, @Valid @RequestBody RecibirPedidoRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.corregir(id, dto));
    }

    @GetMapping("/mios")
    public ResponseEntity<List<PedidoResponseDTO>> listarMios() { return ResponseEntity.ok(pedidoService.listarMios()); }

    @GetMapping("/pendientes")
    public ResponseEntity<List<PedidoResponseDTO>> listarPendientes() { return ResponseEntity.ok(pedidoService.listarPendientes()); }

    @GetMapping("/transito")
    public ResponseEntity<List<PedidoResponseDTO>> listarEnTransito() { return ResponseEntity.ok(pedidoService.listarEnTransito()); }

    @GetMapping("/despachos")
    public ResponseEntity<List<PedidoResponseDTO>> listarDespachos() { return ResponseEntity.ok(pedidoService.listarDespachos()); }

    @GetMapping("/validados")
    public ResponseEntity<List<PedidoResponseDTO>> listarValidados() { return ResponseEntity.ok(pedidoService.listarValidados()); }

    @GetMapping("/activables")
    public ResponseEntity<List<PedidoResponseDTO>> listarActivables() { return ResponseEntity.ok(pedidoService.listarActivables()); }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> obtenerPedido(@PathVariable Long id) { return ResponseEntity.ok(pedidoService.obtener(id)); }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialPedidoResponseDTO>> historial(@PathVariable Long id) { return ResponseEntity.ok(pedidoService.historial(id)); }

    @GetMapping("/tracking/{numeroTracking}")
    public ResponseEntity<PedidoResponseDTO> obtenerPorTracking(@PathVariable String numeroTracking) {
        return ResponseEntity.ok(pedidoService.obtenerPorTracking(numeroTracking));
    }

    @PutMapping("/{id}/validar")
    public ResponseEntity<PedidoResponseDTO> validarPedido(@PathVariable Long id,
            @Valid @RequestBody ValidarPedidoRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.validar(id, dto));
    }

    @PutMapping("/{id}/activar-tracking")
    public ResponseEntity<PedidoResponseDTO> activarTracking(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.activarTracking(id));
    }

    @PutMapping("/{id}/estado-logistico")
    public ResponseEntity<PedidoResponseDTO> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambiarEstadoLogisticoRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.cambiarEstadoLogistico(id, dto));
    }

    @PostMapping("/{id}/etiqueta")
    public ResponseEntity<EtiquetaEnvioResponseDTO> generarEtiqueta(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.generarEtiqueta(id));
    }
}
