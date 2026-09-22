package com.udea.demo.usuarios.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.demo.usuarios.application.dto.LoginRequestDTO;
import com.udea.demo.usuarios.application.dto.LoginResponseDTO;
import com.udea.demo.usuarios.application.dto.RefreshTokenRequestDTO;
import com.udea.demo.usuarios.application.dto.RestablecerPasswordDTO;
import com.udea.demo.usuarios.application.dto.SolicitarRestablecimientoPasswordDTO;
import com.udea.demo.usuarios.domain.exception.CredencialesInvalidasException;
import com.udea.demo.usuarios.domain.exception.CuentaBloqueadaLoginException;
import com.udea.demo.usuarios.domain.exception.PasswordDebilException;
import com.udea.demo.usuarios.domain.exception.PasswordNoCoincideException;
import com.udea.demo.usuarios.domain.exception.SesionInvalidaException;
import com.udea.demo.usuarios.domain.exception.TokenRestablecimientoInvalidoException;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.IntentoInicioSesion;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.SesionUsuario;
import com.udea.demo.usuarios.domain.model.TokenRestablecimientoPassword;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.IntentoInicioSesionRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.TokenRestablecimientoPasswordRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import com.udea.demo.usuarios.interfaces.services.AutenticacionServiceI;
import com.udea.demo.usuarios.interfaces.services.EmailServiceI;

@Service
public class AutenticacionService implements AutenticacionServiceI {
    private static final Logger log = LoggerFactory.getLogger(AutenticacionService.class);
    private static final int MAX_INTENTOS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final IntentoInicioSesionRepository intentoRepository;
    private final SesionUsuarioRepository sesionRepository;
    private final TokenRestablecimientoPasswordRepository resetRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceI emailService;

    @Value("${app.auth.failed-attempt-window-minutes:10}")
    private long ventanaIntentosMinutos;
    @Value("${app.auth.lock-minutes:15}")
    private long minutosBloqueo;
    @Value("${app.auth.refresh-token-days:7}")
    private long diasRefresh;
    @Value("${app.auth.password-reset-minutes:45}")
    private long minutosReset;
    @Value("${app.auth.password-reset-url:http://localhost:8080/restablecer-password}")
    private String passwordResetUrl;
    @Value("${app.auth.client-inactivity-minutes:30}")
    private long minutosInactividadCliente;
    @Value("${app.auth.operational-inactivity-minutes:15}")
    private long minutosInactividadOperativo;

    public AutenticacionService(UsuarioRepository usuarioRepository,
                                IntentoInicioSesionRepository intentoRepository,
                                SesionUsuarioRepository sesionRepository,
                                TokenRestablecimientoPasswordRepository resetRepository,
                                PasswordEncoder passwordEncoder,
                                EmailServiceI emailService) {
        this.usuarioRepository = usuarioRepository;
        this.intentoRepository = intentoRepository;
        this.sesionRepository = sesionRepository;
        this.resetRepository = resetRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional(noRollbackFor = {CredencialesInvalidasException.class, CuentaBloqueadaLoginException.class})
    public LoginResponseDTO iniciarSesion(LoginRequestDTO request, String ip, String userAgent) {
        LocalDateTime ahora = LocalDateTime.now();
        IntentoInicioSesion bloqueo = intentoRepository
                .findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(request.email(), ahora);
        if (bloqueo != null) {
            throw new CuentaBloqueadaLoginException(bloqueo.getBloqueadoHasta());
        }

        Usuario usuario = usuarioRepository.findByEmail(request.email()).orElse(null);
        if (usuario == null || usuario.getEstado() != EstadoUsuario.ACTIVO
                || !Boolean.TRUE.equals(usuario.getActivo())
                || !passwordEncoder.matches(request.password(), usuario.getPassword())) {
            registrarFallo(request.email(), usuario, ahora, ip, userAgent);
            throw new CredencialesInvalidasException();
        }

        intentoRepository.deleteByEmail(request.email());
        return crearSesion(usuario, ahora);
    }

    @Override
    @Transactional
    public LoginResponseDTO renovarSesion(RefreshTokenRequestDTO request) {
        LocalDateTime ahora = LocalDateTime.now();
        SesionUsuario sesion = sesionRepository.findByRefreshTokenHash(hash(request.refreshToken()))
                .orElseThrow(SesionInvalidaException::new);
        if (sesion.getRevokedAt() != null || sesion.getRefreshTokenExpiresAt().isBefore(ahora)
                || !Boolean.TRUE.equals(sesion.getUsuario().getActivo())
                || sesion.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new SesionInvalidaException();
        }
        sesion.setRevokedAt(ahora);
        return crearSesion(sesion.getUsuario(), ahora);
    }

    @Override
    @Transactional
    public void cerrarSesion(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        sesionRepository.findByAccessTokenHash(hash(accessToken)).ifPresent(s -> {
            s.setRevokedAt(LocalDateTime.now());
            sesionRepository.save(s);
        });
    }

    @Override
    @Transactional
    public void solicitarRestablecimiento(SolicitarRestablecimientoPasswordDTO request) {
        usuarioRepository.findByEmail(request.email()).ifPresent(usuario -> {
            resetRepository.deleteByUsuarioId(usuario.getId());
            String token = generarToken();
            resetRepository.save(new TokenRestablecimientoPassword(
                    hash(token), usuario, LocalDateTime.now().plusMinutes(minutosReset)));
                emailService.enviarEnlaceRestablecimiento(request.email(), passwordResetUrl + "/" + token);
        });
    }

    @Override
    @Transactional
    public void restablecerPassword(RestablecerPasswordDTO request) {
        if (!request.nuevaPassword().equals(request.confirmarPassword())) {
            throw new PasswordNoCoincideException();
        }
        validarPassword(request.nuevaPassword());

        TokenRestablecimientoPassword token = resetRepository.findByTokenHash(hash(request.token()))
                .orElseThrow(TokenRestablecimientoInvalidoException::new);
        if (token.getUsadoEn() != null || token.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new TokenRestablecimientoInvalidoException();
        }

        Usuario usuario = token.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getActivo())
                || usuario.getEstado() == EstadoUsuario.INACTIVO
                || usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new TokenRestablecimientoInvalidoException();
        }
        usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        // Internal users created by CLI can activate their account through the existing
        // password recovery flow; unverified client accounts still require email verification.
        if (usuario.getEstado() == EstadoUsuario.PENDIENTE_ACTIVACION) {
            usuario.setEstado(EstadoUsuario.ACTIVO);
        }
        usuarioRepository.save(usuario);
        token.setUsadoEn(LocalDateTime.now());
        resetRepository.save(token);
        sesionRepository.deleteByUsuarioId(usuario.getId());
        intentoRepository.deleteByEmail(usuario.getEmail());
    }

    private void registrarFallo(String email, Usuario usuario, LocalDateTime ahora,
                                String ip, String userAgent) {
        LocalDateTime desde = ahora.minusMinutes(ventanaIntentosMinutos);
        long intentos = intentoRepository.countByEmailAndIntentadoEnAfter(email, desde) + 1;
        LocalDateTime bloqueadoHasta = intentos >= MAX_INTENTOS
                ? ahora.plusMinutes(minutosBloqueo) : null;
        intentoRepository.save(new IntentoInicioSesion(email, usuario, ahora, bloqueadoHasta));
        log.warn("Intento de inicio de sesión fallido para {} desde {} ({})", email, ip, userAgent);
        if (bloqueadoHasta != null) {
            throw new CuentaBloqueadaLoginException(bloqueadoHasta);
        }
    }

    private LoginResponseDTO crearSesion(Usuario usuario, LocalDateTime ahora) {
        String accessToken = generarToken();
        String refreshToken = generarToken();
        long minutosInactividad = usuario.getRol() == Rol.CLIENTE
            ? minutosInactividadCliente : minutosInactividadOperativo;
        LocalDateTime accessExpira = ahora.plusMinutes(minutosInactividad);
        LocalDateTime refreshExpira = ahora.plusDays(diasRefresh);
        sesionRepository.save(new SesionUsuario(usuario, hash(accessToken), hash(refreshToken),
                accessExpira, refreshExpira, ahora));
        return new LoginResponseDTO(accessToken, refreshToken, accessExpira, refreshExpira,
                usuario.getRol(), panelDe(usuario.getRol()));
    }

    private void validarPassword(String password) {
        if (password.length() < 8 || !password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).*$")) {
            throw new PasswordDebilException("La contraseña debe tener al menos 8 caracteres, mayúscula, minúscula, número y carácter especial");
        }
    }

    private String panelDe(Rol rol) {
        return switch (rol) {
            case CLIENTE -> "/panel/cliente";
            case OPERADOR -> "/panel/operador";
            case CONDUCTOR -> "/panel/conductor";
        };
    }

    private String generarToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo generar el hash de seguridad", ex);
        }
    }
}