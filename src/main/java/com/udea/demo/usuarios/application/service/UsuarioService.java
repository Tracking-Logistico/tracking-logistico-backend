package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.RegistroClienteRequestDTO;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.infrastructure.persistence.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public UsuarioResponseDTO registrarCliente(RegistroClienteRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado: " + dto.email());
        }

        Usuario usuario = Usuario.builder()
                .nombre(dto.nombre())
                .email(dto.email())
                .password(dto.password())
                .telefono(dto.telefono())
                .direccion(dto.direccion())
                .rol(Rol.CLIENTE)
                .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                .aceptoTerminos(dto.aceptoTerminos())
                .fechaAceptacionTerminos(LocalDateTime.now())
                .versionTerminos(dto.versionTerminos())
                .activo(true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);

        return new UsuarioResponseDTO(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getEmail(),
                guardado.getTelefono(),
                guardado.getDireccion(),
                guardado.getRol(),
                guardado.getEstado(),
                guardado.getFechaCreacion()
        );
    }
}