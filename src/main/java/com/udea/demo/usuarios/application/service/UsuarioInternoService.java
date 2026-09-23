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
    private final ClienteRepository clienteRepository;
    private final ConductorRepository conductorRepository;
    private final OperadorRepository operadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordTemporalService passwordTemporalService;
    private final ActorAuthorizationService actorAuthorizationService;
    private final SesionUsuarioRepository sesionRepository;
    private final PasswordPolicyService passwordPolicyService;

    public UsuarioInternoService(UsuarioRepository usuarioRepository,
                                 ClienteRepository clienteRepository,
                                 ConductorRepository conductorRepository,
                                 OperadorRepository operadorRepository,
                                 PasswordEncoder passwordEncoder,
                                 PasswordTemporalService passwordTemporalService,
                                 ActorAuthorizationService actorAuthorizationService,
                                 SesionUsuarioRepository sesionRepository,
                                 PasswordPolicyService passwordPolicyService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.conductorRepository = conductorRepository;
        this.operadorRepository = operadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordTemporalService = passwordTemporalService;
        this.actorAuthorizationService = actorAuthorizationService;
        this.sesionRepository = sesionRepository;
        this.passwordPolicyService = passwordPolicyService;
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

        Rol destino = command.rol() == null ? usuario.getRol() : command.rol();
        // La identidad de cada rol tiene su propia tabla. Conservar las filas anteriores:
        // pedidos, rutas e historial pueden seguir referenciándolas.
        if (destino == Rol.CONDUCTOR) {
            conductorRepository.findByUsuarioId(id).ifPresentOrElse(conductor -> {
                if (command.licencia() != null) {
                    if (esVacio(command.licencia())) throw new LicenciaRequeridaException();
                    conductor.setLicencia(command.licencia());
                    conductorRepository.save(conductor);
                }
            }, () -> {
                if (esVacio(command.licencia())) throw new LicenciaRequeridaException();
                crearFilaSegunRol(usuario, Rol.CONDUCTOR, command.licencia(), null);
            });
        } else if (destino == Rol.OPERADOR) {
            operadorRepository.findByUsuarioId(id).ifPresentOrElse(operador -> {
                if (command.codigoEmpleado() != null) {
                    if (esVacio(command.codigoEmpleado())) throw new CodigoEmpleadoRequeridoException();
                    if (!command.codigoEmpleado().equals(operador.getCodigoEmpleado())
                            && operadorRepository.existsByCodigoEmpleado(command.codigoEmpleado())) {
                        throw new IllegalArgumentException("El código de empleado ya está registrado");
                    }
                    operador.setCodigoEmpleado(command.codigoEmpleado());
                    operadorRepository.save(operador);
                }
            }, () -> {
                if (esVacio(command.codigoEmpleado())) throw new CodigoEmpleadoRequeridoException();
                if (operadorRepository.existsByCodigoEmpleado(command.codigoEmpleado())) {
                    throw new IllegalArgumentException("El código de empleado ya está registrado");
                }
                crearFilaSegunRol(usuario, Rol.OPERADOR, null, command.codigoEmpleado());
            });
        } else if (destino == Rol.CLIENTE) {
            if (clienteRepository.findByUsuarioId(id).isEmpty()) {
                clienteRepository.save(Cliente.builder().usuario(usuario).build());
            }
        } else {
            throw new RolInternoInvalidoException();
        }

        if (destino != usuario.getRol()) {
            usuario.setRol(destino);
            // El correo es opcional: una cuenta cliente pendiente de verificación
            // sigue activa cuando pasa a ser operador o conductor.
            if (usuario.getEstado() == EstadoUsuario.PENDIENTE_VERIFICACION) {
                usuario.setEstado(EstadoUsuario.ACTIVO);
            }
            // Una sesión antigua nunca debe conservar acceso con un rol nuevo.
            sesionRepository.deleteByUsuarioId(id);
        }

        return mapToDTO(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        if (usuario.getRol() != Rol.OPERADOR && usuario.getRol() != Rol.CONDUCTOR) {
            throw new RolInternoInvalidoException();
        }
        usuario.setActivo(false);
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
        // Se conservan entidades operativas para mantener historial y referencias de rutas.
        sesionRepository.deleteByUsuarioId(usuario.getId());
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
        passwordPolicyService.validar(nuevaPassword, usuario.getEmail(), usuario.getNombre());

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
