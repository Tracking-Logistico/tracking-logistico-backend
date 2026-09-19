package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.application.dto.EtiquetaEnvioResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.RecibirPedidoRequestDTO;
import com.udea.demo.pedidos.application.dto.ValidarPedidoRequestDTO;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoServiceI pedidoService;

    public PedidoController(PedidoServiceI pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Criterio 1: Recepción Automática
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> recibirPedido(@Valid @RequestBody RecibirPedidoRequestDTO dto) {
        PedidoResponseDTO respuesta = pedidoService.recibir(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // Criterio 2: Visualización de Pendientes (bandeja de entrada del operador)
    @GetMapping("/pendientes")
    public ResponseEntity<List<PedidoResponseDTO>> listarPendientes() {
        return ResponseEntity.ok(pedidoService.listarPendientes());
    }

    @GetMapping("/transito")
    public ResponseEntity<List<PedidoResponseDTO>> listarEnTransito() {
        return ResponseEntity.ok(pedidoService.listarEnTransito());
    }

    @GetMapping("/validados")
    public ResponseEntity<List<PedidoResponseDTO>> listarValidados() {
        return ResponseEntity.ok(pedidoService.listarValidados());
    }

    @GetMapping("/activables")
    public ResponseEntity<List<PedidoResponseDTO>> listarActivables() {
        return ResponseEntity.ok(pedidoService.listarActivables());
    }

    // Criterio 2: Validación de la información suministrada antes de decidir
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtener(id));
    }

    @GetMapping("/tracking/{numeroTracking}")
    public ResponseEntity<PedidoResponseDTO> obtenerPorTracking(@PathVariable String numeroTracking) {
        return ResponseEntity.ok(pedidoService.obtenerPorTracking(numeroTracking));
    }

    // Criterio 3: Sugerencia y Asignación de Prioridad (confirmar o ajustar)
    @PutMapping("/{id}/validar")
    public ResponseEntity<PedidoResponseDTO> validarPedido(
            @PathVariable Long id,
            @Valid @RequestBody ValidarPedidoRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.validar(id, dto));
    }

    // Criterio 1: Activación de Tracking
    @PutMapping("/{id}/activar-tracking")
    public ResponseEntity<PedidoResponseDTO> activarTracking(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.activarTracking(id));
    }

    // Criterio 2: Generación e Impresión de Etiqueta
    @PostMapping("/{id}/etiqueta")
    public ResponseEntity<EtiquetaEnvioResponseDTO> generarEtiqueta(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.generarEtiqueta(id));
    }
}
