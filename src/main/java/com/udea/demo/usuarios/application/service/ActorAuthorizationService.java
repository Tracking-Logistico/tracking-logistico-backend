package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.ClienteRepository;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import com.udea.demo.usuarios.interfaces.persistence.OperadorRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ActorAuthorizationService {
    private final UsuarioRepository usuarios;
    private final OperadorRepository operadores;
    private final ClienteRepository clientes;
    private final ConductorRepository conductores;

    public ActorAuthorizationService(UsuarioRepository usuarios, OperadorRepository operadores,
                                     ClienteRepository clientes, ConductorRepository conductores) {
        this.usuarios = usuarios;
        this.operadores = operadores;
        this.clientes = clientes;
        this.conductores = conductores;
    }

    public void exigirPropietario(Long usuarioId, Rol rol) {
        Usuario actor = actorActual();
        if (actor.getRol() != rol || !actor.getId().equals(usuarioId)) {
            throw new AccessDeniedException("No puedes modificar recursos de otro usuario");
        }
    }

    public Long clienteActualId() {
        Usuario actor = exigirRol(Rol.CLIENTE);
        return clientes.findByUsuarioId(actor.getId())
                .orElseThrow(() -> new AccessDeniedException("La sesión no corresponde a un cliente válido"))
                .getId();
    }

    public Long operadorActualId() {
        Usuario actor = exigirRol(Rol.OPERADOR);
        return operadores.findByUsuarioId(actor.getId())
                .orElseThrow(() -> new AccessDeniedException("La sesión no corresponde a un operador válido"))
                .getId();
    }

    public Long conductorActualId() {
        Usuario actor = exigirRol(Rol.CONDUCTOR);
        return conductores.findByUsuarioId(actor.getId())
                .orElseThrow(() -> new AccessDeniedException("La sesión no corresponde a un conductor válido"))
                .getId();
    }

    public Usuario actorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof String email)) {
            throw new AccessDeniedException("No existe una sesión válida");
        }
        Usuario actor = usuarios.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("No existe una sesión válida"));
        if (!Boolean.TRUE.equals(actor.getActivo())) {
            throw new AccessDeniedException("No existe una sesión válida");
        }
        return actor;
    }

    private Usuario exigirRol(Rol rol) {
        Usuario actor = actorActual();
        if (actor.getRol() != rol) {
            throw new AccessDeniedException("El rol de la sesión no permite esta operación");
        }
        return actor;
    }
}
