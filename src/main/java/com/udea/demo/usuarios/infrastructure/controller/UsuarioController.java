package com.udea.demo.usuarios.infrastructure.controller;

import com.udea.demo.usuarios.application.dto.ActualizarPerfilRequestDTO;
import com.udea.demo.usuarios.application.dto.RegistroClienteRequestDTO;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.application.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrarCliente(@Valid @RequestBody RegistroClienteRequestDTO dto) {
        UsuarioResponseDTO respuesta = usuarioService.registrarCliente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/verificar")
    public ResponseEntity<String> verificarCuenta(@RequestParam String token) {
        usuarioService.verificarCuenta(token);
        return ResponseEntity.ok("Cuenta verificada con éxito. Ya puedes iniciar sesión.");
    }

    @PutMapping("/{id}/perfil")
    public ResponseEntity<UsuarioResponseDTO> actualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPerfilRequestDTO dto) {
        UsuarioResponseDTO respuesta = usuarioService.actualizarPerfil(id, dto);
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarCuenta(@PathVariable Long id) {
        usuarioService.desactivarCuenta(id);
        return ResponseEntity.noContent().build();
    }
}