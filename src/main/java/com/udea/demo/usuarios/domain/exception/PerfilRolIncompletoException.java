package com.udea.demo.usuarios.domain.exception;

import com.udea.demo.usuarios.domain.model.Rol;

/** El usuario tiene el rol, pero no tiene su perfil en la tabla correspondiente. */
public class PerfilRolIncompletoException extends RuntimeException {
    private final Rol rol;
    private final Long usuarioId;

    public PerfilRolIncompletoException(Rol rol, Long usuarioId) {
        super("El usuario tiene rol " + rol + " pero no tiene registro en su tabla de perfil");
        this.rol = rol;
        this.usuarioId = usuarioId;
    }

    public Rol getRol() { return rol; }
    public Long getUsuarioId() { return usuarioId; }
}
