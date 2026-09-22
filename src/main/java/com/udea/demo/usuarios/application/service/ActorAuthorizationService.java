package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.OperadorRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ActorAuthorizationService {
    private final UsuarioRepository usuarios;
    private final OperadorRepository operadores;

    public ActorAuthorizationService(UsuarioRepository usuarios, OperadorRepository operadores) {
        this.usuarios = usuarios;
        this.operadores = operadores;
    }

    public void exigirPropietario(Long usuarioId, Rol rol) {
        Usuario actor = actorActual();
        if (actor.getRol() != rol || !actor.getId().equals(usuarioId)) {
            throw new AccessDeniedException("No puedes modificar recursos de otro usuario");
        }
    }

    public void exigirOperador(Long operadorId) {
        Usuario actor = actorActual();
        if (actor.getRol() != Rol.OPERADOR || operadores.findByUsuarioId(actor.getId())
                .filter(operador -> operador.getId().equals(operadorId)).isEmpty()) {
            throw new AccessDeniedException("El operador indicado no corresponde a la sesión");
        }
    }

    private Usuario actorActual() {
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
}
