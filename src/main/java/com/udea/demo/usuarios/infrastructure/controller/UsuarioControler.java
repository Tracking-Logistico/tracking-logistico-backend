package com.udea.demo.usuarios.infrastructure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.udea.demo.usuarios.application.dto.CambiarPasswordRequestDTO;
import com.udea.demo.usuarios.interfaces.services.UsuarioInternoServiceI;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioControler {
    private final UsuarioInternoServiceI usuarioInternoService;
    public UsuarioControler(UsuarioInternoServiceI usuarioInternoService) {
        this.usuarioInternoService = usuarioInternoService;
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<String> cambiarPassword(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPasswordRequestDTO dto) {

        usuarioInternoService.cambiarPassword(
                id,
                dto.passwordActual(),
                dto.nuevaPassword(),
                dto.confirmarPassword()
        );

        return ResponseEntity.ok("Contraseña actualizada correctamente.");
    }

}
