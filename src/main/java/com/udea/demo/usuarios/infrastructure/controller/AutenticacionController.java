package com.udea.demo.usuarios.infrastructure.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.udea.demo.usuarios.application.dto.*;
import com.udea.demo.usuarios.interfaces.services.AutenticacionServiceI;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacionController {
    private final AutenticacionServiceI autenticacionService;

    public AutenticacionController(AutenticacionServiceI autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request, HttpServletRequest httpRequest) {
        return autenticacionService.iniciarSesion(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    public LoginResponseDTO refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return autenticacionService.renovarSesion(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        autenticacionService.cerrarSesion(extraerBearer(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<String> forgot(@Valid @RequestBody SolicitarRestablecimientoPasswordDTO request) {
        autenticacionService.solicitarRestablecimiento(request);
        return ResponseEntity.ok("Si el correo existe, intentaremos enviar instrucciones para restablecer tu contraseña");
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody RestablecerPasswordDTO request) {
        autenticacionService.restablecerPassword(request);
        return ResponseEntity.noContent().build();
    }

    private String extraerBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }
}