package com.udea.demo.usuarios.infrastructure.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.udea.demo.usuarios.interfaces.services.EmailServiceI;

@Service
public class EmailService implements EmailServiceI {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarEnlaceRestablecimiento(String destinatario, String enlace) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Restablecimiento de contraseña");
        mensaje.setText("Recibimos una solicitud para restablecer tu contraseña.\n\n"
                + "Usa el siguiente enlace dentro de los próximos 45 minutos:\n"
                + enlace + "\n\n"
                + "Si no solicitaste este cambio, puedes ignorar este mensaje.");
        mailSender.send(mensaje);
    }
}