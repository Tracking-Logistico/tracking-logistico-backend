package com.udea.demo.usuarios.infrastructure.controller;

import com.udea.demo.usuarios.application.dto.ActualizarPerfilRequestDTO;
import com.udea.demo.usuarios.application.dto.ClienteResponseDTO;
import com.udea.demo.usuarios.application.dto.RegistroClienteRequestDTO;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.interfaces.services.ClienteServiceI;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteServiceI clienteService;
    

    public ClienteController(ClienteServiceI usuarioService) {
        this.clienteService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<ClienteResponseDTO> registrarCliente(@Valid @RequestBody RegistroClienteRequestDTO dto) {
        ClienteResponseDTO respuesta = clienteService.registrarCliente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/verificar")
    public ResponseEntity<String> verificarCuenta(@RequestParam String token) {
        clienteService.verificarCuenta(token);
        return ResponseEntity.ok("Correo verificado correctamente. Puedes seguir utilizando tu cuenta.");
    }

    @PostMapping("/verificacion/reenviar")
    public ResponseEntity<String> reenviarVerificacion(@Valid @RequestBody
            com.udea.demo.usuarios.application.dto.SolicitarRestablecimientoPasswordDTO dto) {
        clienteService.reenviarVerificacion(dto.email());
        return ResponseEntity.ok("Si existe una cuenta pendiente, recibirás un enlace de verificación.");
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ClienteResponseDTO> obtenerPerfilActual() {
        return ResponseEntity.ok(clienteService.obtenerPerfilActual());
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/me/perfil")
    public ResponseEntity<UsuarioResponseDTO> actualizarPerfilActual(@Valid @RequestBody ActualizarPerfilRequestDTO dto) {
        return ResponseEntity.ok(clienteService.actualizarPerfilActual(dto));
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    public ResponseEntity<Void> desactivarCuentaActual() {
        clienteService.desactivarCuentaActual();
        return ResponseEntity.noContent().build();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}/perfil")
    public ResponseEntity<UsuarioResponseDTO> actualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPerfilRequestDTO dto) {
        UsuarioResponseDTO respuesta = clienteService.actualizarPerfil(id, dto);
        return ResponseEntity.ok(respuesta);
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarCuenta(@PathVariable Long id) {
        clienteService.desactivarCuentaCliente(id);
        return ResponseEntity.noContent().build();
    }


}