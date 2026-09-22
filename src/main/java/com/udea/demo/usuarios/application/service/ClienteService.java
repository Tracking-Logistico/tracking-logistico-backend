package com.udea.demo.usuarios.application.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.demo.usuarios.application.dto.ActualizarPerfilRequestDTO;
import com.udea.demo.usuarios.application.dto.ClienteResponseDTO;
import com.udea.demo.usuarios.application.dto.RegistroClienteRequestDTO;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.domain.model.Cliente;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.TokenVerificacion;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.ClienteRepository;
import com.udea.demo.usuarios.interfaces.persistence.TokenVerificacionRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import com.udea.demo.usuarios.interfaces.services.ClienteServiceI;

@Service
public class ClienteService implements ClienteServiceI {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final TokenVerificacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public ClienteService(UsuarioRepository usuarioRepository,
                          ClienteRepository clienteRepository,
                          TokenVerificacionRepository tokenRepository,
                          PasswordEncoder passwordEncoder,
                          JavaMailSender mailSender) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @Override 
    @Transactional
    public ClienteResponseDTO registrarCliente(RegistroClienteRequestDTO dto) {
        if (!dto.password().equals(dto.confirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(dto.nombre())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .telefono(dto.telefono())
                .direccion(dto.direccion())
                .rol(Rol.CLIENTE)
                .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                .activo(true)
                .aceptoTerminos(dto.aceptoTerminos())
                .versionTerminos(dto.versionTerminos())
                .fechaAceptacionTerminos(LocalDateTime.now())
                .fechaCreacion(LocalDateTime.now())
                .build();

        Usuario guardado = usuarioRepository.save(usuario);

        Cliente cliente = Cliente.builder()
                .usuario(guardado)
                .ciudad(dto.ciudad())
                .build();
        clienteRepository.save(cliente);

        String tokenUUID = UUID.randomUUID().toString();
        TokenVerificacion tokenVerificacion = TokenVerificacion.builder()
                .token(tokenUUID)
                .usuario(guardado)
                .fechaExpiracion(LocalDateTime.now().plusHours(2))
                .build();

        tokenRepository.save(tokenVerificacion);

        enviarCorreoVerificacion(guardado.getEmail(), guardado.getNombre(), tokenUUID);

        return mapToClienteResponseDTO(cliente);
    }

    private void enviarCorreoVerificacion(String emailDestino, String nombreUsuario, String token) {
        String urlVerificacion = "https://tracking-logistico-backend.onrender.com/api/v1/clientes/verificar?token=" + token;

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("Verificación de Cuenta - Tracking Logístico");
        mensaje.setText("Hola " + nombreUsuario + ",\n\n"
                + "¡Gracias por registrarte! Para activar tu cuenta, ingresa al siguiente enlace:\n"
                + urlVerificacion + "\n\n"
                + "Este enlace expira en 2 horas.");

        mailSender.send(mensaje);
    }

    @Override 
    @Transactional
    public void verificarCuenta(String token) {
        TokenVerificacion tokenVerificacion = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de verificación inválido"));

        if (tokenVerificacion.estaExpirado()) {
            throw new IllegalArgumentException("El token de verificación ha expirado");
        }

        Usuario usuario = tokenVerificacion.getUsuario();
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(usuario);

        tokenRepository.delete(tokenVerificacion);
    }

    @Override 
    @Transactional
    public UsuarioResponseDTO actualizarPerfil(Long id, ActualizarPerfilRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        usuario.setNombre(dto.nombre());
        if (dto.telefono() != null) usuario.setTelefono(dto.telefono());
        if (dto.direccion() != null) usuario.setDireccion(dto.direccion());

        Usuario actualizado = usuarioRepository.save(usuario);

        return mapToUsuarioDTO(actualizado);
    }

    @Override
    @Transactional
    public void desactivarCuentaCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + id));
        Usuario u = cliente.getUsuario();
        u.setActivo(false);
        u.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(u);
    }

    private ClienteResponseDTO mapToClienteResponseDTO(Cliente c) {
        return new ClienteResponseDTO(
                c.getId(),
                c.getUsuario().getNombre(),
                c.getUsuario().getEmail(),
                c.getUsuario().getTelefono(),
                c.getUsuario().getDireccion(),
                c.getCiudad(),
                c.getUsuario().getRol(),
                c.getUsuario().getEstado(),
                c.getUsuario().getFechaCreacion()
        );
    }

    private UsuarioResponseDTO mapToUsuarioDTO(Usuario u) {
        return new UsuarioResponseDTO(
                u.getId(), u.getNombre(), u.getEmail(),
                u.getTelefono(), u.getDireccion(),
                u.getRol(), u.getEstado(), u.getFechaCreacion());
    }
}