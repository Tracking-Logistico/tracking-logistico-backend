package com.udea.demo.usuarios.infrastructure.controller;

import com.udea.demo.usuarios.application.dto.ActualizarRolUsuarioRequestDTO;
import com.udea.demo.usuarios.application.dto.EditarUsuarioInternoCommand;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import com.udea.demo.usuarios.interfaces.services.UsuarioInternoServiceI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class AdministracionUsuariosController {
    private final UsuarioRepository usuarios;
    private final UsuarioInternoServiceI usuariosInternos;

    public AdministracionUsuariosController(UsuarioRepository usuarios, UsuarioInternoServiceI usuariosInternos) {
        this.usuarios = usuarios;
        this.usuariosInternos = usuariosInternos;
    }

    @GetMapping
    public List<UsuarioResponseDTO> listar(@RequestParam(required = false) Rol rol) {
        return usuarios.findAll().stream()
                .filter(u -> rol == null || u.getRol() == rol)
                .map(u -> new UsuarioResponseDTO(u.getId(), u.getNombre(), u.getEmail(), u.getTelefono(),
                        u.getDireccion(), u.getRol(), u.getEstado(), u.getFechaCreacion()))
                .toList();
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponseDTO> actualizarRol(@PathVariable Long id,
            @Valid @RequestBody ActualizarRolUsuarioRequestDTO request) {
        if (request.rol() != Rol.OPERADOR && request.rol() != Rol.CONDUCTOR) {
            throw new IllegalArgumentException("Solo se puede asignar el rol OPERADOR o CONDUCTOR a usuarios internos");
        }
        var command = new EditarUsuarioInternoCommand(null, null, null, request.rol(),
                request.licencia(), request.codigoEmpleado());
        return ResponseEntity.ok(usuariosInternos.editar(id, command));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        usuariosInternos.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
