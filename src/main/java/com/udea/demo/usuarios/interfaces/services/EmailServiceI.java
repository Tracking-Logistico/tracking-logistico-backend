package com.udea.demo.usuarios.interfaces.services;

public interface EmailServiceI {
    void enviarEnlaceRestablecimiento(String destinatario, String enlace);
}