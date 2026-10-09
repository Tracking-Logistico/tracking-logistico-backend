package com.udea.demo.usuarios.infrastructure.controller;

import com.udea.demo.usuarios.application.dto.ActualizarPreferenciaRequestDTO;
import com.udea.demo.usuarios.application.dto.PreferenciaResponseDTO;
import com.udea.demo.usuarios.interfaces.services.PreferenciaServiceI;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/notificaciones")
public class PreferenciaController {

    private final PreferenciaServiceI service;

    public PreferenciaController(PreferenciaServiceI service) {
        this.service = service;
    }

    @GetMapping("/preferencias")
    public ResponseEntity<PreferenciaResponseDTO> obtener() {
        return ResponseEntity.ok(service.obtener());
    }

    @PutMapping("/preferencias")
    public ResponseEntity<PreferenciaResponseDTO> actualizar(
            @Valid @RequestBody ActualizarPreferenciaRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(dto));
    }
}