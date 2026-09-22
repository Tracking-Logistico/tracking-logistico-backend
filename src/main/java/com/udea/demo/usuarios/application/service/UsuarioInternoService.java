package com.udea.demo.usuarios.application.service;


import com.udea.demo.usuarios.application.dto.*;
import com.udea.demo.usuarios.domain.exception.*;
import com.udea.demo.usuarios.domain.model.*;
import com.udea.demo.usuarios.interfaces.persistence.*;
import com.udea.demo.usuarios.interfaces.services.UsuarioInternoServiceI;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UsuarioInternoService implements UsuarioInternoServiceI {

    private final UsuarioRepository usuarioRepository;
    private final ConductorRepository conductorRepository;
    private final OperadorRepository operadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordTemporalService passwordTemporalService;
    private final ActorAuthorizationService actorAuthorizationService;
    private final SesionUsuarioRepository sesionRepository;

    public UsuarioInternoService(UsuarioRepository usuarioRepository,
                                 ConductorRepository conductorRepository,
                                 OperadorRepository operadorRepository,
                                 PasswordEncoder passwordEncoder,
                                 PasswordTemporalService passwordTemporalService,
                                 ActorAuthorizationService actorAuthorizationService,
                                 SesionUsuarioRepository sesionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.conductorRepository = conductorRepository;
        this.operadorRepository = operadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordTemporalService = passwordTemporalService;
        this.actorAuthorizationService = actorAuthorizationService;
        this.sesionRepository = sesionRepository;
    }

    @Override
    @Transactional
    public ResultadoCreacionUsuarioInterno crear(CrearUsuarioInternoCommand command) {

        if (command.rol() != Rol.OPERADOR && command.rol() != Rol.CONDUCTOR) {
            throw new RolInternoInvalidoException();
        }

        // validaciones específicas por rol
        if (command.rol() == Rol.CONDUCTOR && esVacio(command.licencia())) {
            throw new LicenciaRequeridaException();
        }
        if (command.rol() == Rol.OPERADOR && esVacio(command.codigoEmpleado())) {
            throw new CodigoEmpleadoRequeridoException();
        }

        if (usuarioRepository.existsByEmail(command.email())) {
            throw new EmailYaRegistradoException(command.email());
        }

        // validar unicidad del código de empleado
        if (command.rol() == Rol.OPERADOR
                && operadorRepository.existsByCodigoEmpleado(command.codigoEmpleado())) {
            throw new CodigoEmpleadoRequeridoException(); // o una excepción de duplicado
        }

        String passwordTemporal = passwordTemporalService.generar();

        Usuario usuario = Usuario.builder()
                .nombre(command.nombre())
                .email(command.email())
                .password(passwordEncoder.encode(passwordTemporal))
                .telefono(command.telefono())
                .direccion(command.direccion())
                .rol(command.rol())
                .estado(EstadoUsuario.PENDIENTE_ACTIVACION)
                .aceptoTerminos(false)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Usuario guardado = usuarioRepository.save(usuario);

        crearFilaSegunRol(guardado, command.rol(), command.licencia(), command.codigoEmpleado());

        return new ResultadoCreacionUsuarioInterno(
                guardado.getId(),
                guardado.getEmail(),
                guardado.getRol(),
                passwordTemporal
        );
    }

    @Override
    @Transactional
    public UsuarioResponseDTO editar(Long id, EditarUsuarioInternoCommand command) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        if (command.nombre() != null) usuario.setNombre(command.nombre());
        if (command.telefono() != null) usuario.setTelefono(command.telefono());
        if (command.direccion() != null) usuario.setDireccion(command.direccion());

        // cambio de rol
        if (command.rol() != null && !command.rol().equals(usuario.getRol())) {
            if (command.rol() != Rol.OPERADOR && command.rol() != Rol.CONDUCTOR) {
                throw new RolInternoInvalidoException();
            }

            // validar datos requeridos para el nuevo rol
            if (command.rol() == Rol.CONDUCTOR && esVacio(command.licencia())) {
                throw new LicenciaRequeridaException();
            }
            if (command.rol() == Rol.OPERADOR && esVacio(command.codigoEmpleado())) {
                throw new CodigoEmpleadoRequeridoException();
            }

            // borrar fila del rol anterior
            if (usuario.getRol() == Rol.CONDUCTOR) {
                conductorRepository.deleteByUsuarioId(usuario.getId());
            } else if (usuario.getRol() == Rol.OPERADOR) {
                operadorRepository.deleteByUsuarioId(usuario.getId());
            }

            // crear fila del nuevo rol
            crearFilaSegunRol(usuario, command.rol(),
                              command.licencia(), command.codigoEmpleado());

            usuario.setRol(command.rol());
        } else {
            // mismo rol: actualizar datos de la tabla específica si vienen
            if (usuario.getRol() == Rol.CONDUCTOR && command.licencia() != null) {
                conductorRepository.findByUsuarioId(usuario.getId())
                        .ifPresent(c -> {
                            c.setLicencia(command.licencia());
                            conductorRepository.save(c);
                        });
            }
            if (usuario.getRol() == Rol.OPERADOR && command.codigoEmpleado() != null) {
                operadorRepository.findByUsuarioId(usuario.getId())
                        .ifPresent(o -> {
                            o.setCodigoEmpleado(command.codigoEmpleado());
                            operadorRepository.save(o);
                        });
            }
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        return mapToDTO(actualizado);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        usuario.setActivo(false);
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
        // no se borran conductor/operador: se conserva historial
    }

    @Override
    @Transactional
    public void cambiarPassword(Long id, String passwordActual,
                                String nuevaPassword, String confirmarPassword) {

        // Password changes are always scoped to the signed-in account.
        // Do not allow a caller to change another user's password using an ID.
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));
        actorAuthorizationService.exigirPropietario(id, usuario.getRol());

        if (!nuevaPassword.equals(confirmarPassword)) {
            throw new PasswordNoCoincideException();
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new CuentaInactivaException();
        }

        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new PasswordDebilException("La contraseña actual no es correcta");
        }

        if (passwordEncoder.matches(nuevaPassword, usuario.getPassword())) {
            throw new PasswordDebilException("La nueva contraseña no puede ser igual a la anterior");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));

        if (usuario.getEstado() == EstadoUsuario.PENDIENTE_ACTIVACION) {
            usuario.setEstado(EstadoUsuario.ACTIVO);
        }

        usuarioRepository.save(usuario);
        sesionRepository.deleteByUsuarioId(usuario.getId());
    }

    // ---------- helpers ----------

    private void crearFilaSegunRol(Usuario usuario, Rol rol,
                                   String licencia, String codigoEmpleado) {
        if (rol == Rol.CONDUCTOR) {
            Conductor c = Conductor.builder()
                    .usuario(usuario)
                    .licencia(licencia)
                    .estado("ACTIVO")
                    .build();
            conductorRepository.save(c);
        } else if (rol == Rol.OPERADOR) {
            Operador o = Operador.builder()
                    .usuario(usuario)
                    .codigoEmpleado(codigoEmpleado)
                    .build();
            operadorRepository.save(o);
        }
    }

    private boolean esVacio(String s) {
        return s == null || s.isBlank();
    }

    private UsuarioResponseDTO mapToDTO(Usuario u) {
        return new UsuarioResponseDTO(
                u.getId(), u.getNombre(), u.getEmail(),
                u.getTelefono(), u.getDireccion(),
                u.getRol(), u.getEstado(), u.getFechaCreacion());
    }
}
