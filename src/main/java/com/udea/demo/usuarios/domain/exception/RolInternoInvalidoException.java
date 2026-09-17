package com.udea.demo.usuarios.domain.exception;


public class RolInternoInvalidoException extends RuntimeException {
    public RolInternoInvalidoException() {
        super("Solo se permite rol OPERADOR o CONDUCTOR para usuarios internos");
    }
}