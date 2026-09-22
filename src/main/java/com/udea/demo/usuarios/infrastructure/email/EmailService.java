package com.udea.demo.usuarios.infrastructure.email;

import com.udea.demo.usuarios.domain.exception.EntregaCorreoException;
import com.udea.demo.usuarios.interfaces.services.EmailServiceI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class EmailService implements EmailServiceI {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final RestClient resend = RestClient.builder()
            .baseUrl("https://api.resend.com")
            .requestFactory(requestFactory())
            .build();

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return factory;
    }

    @Value("${app.mail.provider:resend}") private String provider;
    @Value("${app.mail.resend-api-key:}") private String resendApiKey;
    @Value("${app.mail.from:}") private String from;
    @Value("${spring.mail.username:}") private String smtpUsername;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarVerificacion(String destinatario, String nombreUsuario, String enlace) {
        enviar(destinatario, "Verificación de Cuenta - Tracking Logístico",
                "Hola " + nombreUsuario + ",\n\n"
                + "¡Gracias por registrarte! Para activar tu cuenta, ingresa al siguiente enlace:\n"
                + enlace + "\n\nEste enlace expira en 2 horas.");
    }

    @Override
    public void enviarEnlaceRestablecimiento(String destinatario, String enlace) {
        enviar(destinatario, "Restablecimiento de contraseña",
                "Recibimos una solicitud para restablecer tu contraseña.\n\n"
                + "Usa el siguiente enlace dentro de los próximos 45 minutos:\n"
                + enlace + "\n\nSi no solicitaste este cambio, puedes ignorar este mensaje.");
    }

    private void enviar(String destinatario, String asunto, String contenido) {
        try {
            if ("resend".equalsIgnoreCase(provider)) {
                if (resendApiKey.isBlank() || from.isBlank()) {
                    log.error("El correo por HTTPS requiere RESEND_API_KEY y MAIL_FROM en Render");
                    throw new EntregaCorreoException();
                }
                resend.post().uri("/emails")
                        .header("Authorization", "Bearer " + resendApiKey)
                        .body(Map.of("from", from, "to", List.of(destinatario),
                                     "subject", asunto, "text", contenido))
                        .retrieve().toBodilessEntity();
            } else if ("smtp".equalsIgnoreCase(provider)) {
                String remitente = from.isBlank() ? smtpUsername : from;
                if (remitente.isBlank()) {
                    log.error("El correo SMTP requiere MAIL_FROM o MAIL_USERNAME");
                    throw new EntregaCorreoException();
                }
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setFrom(remitente);
                mensaje.setTo(destinatario);
                mensaje.setSubject(asunto);
                mensaje.setText(contenido);
                mailSender.send(mensaje);
            } else {
                log.error("Proveedor de correo desconocido: {}", provider);
                throw new EntregaCorreoException();
            }
        } catch (RestClientException | MailException ex) {
            // Do not log tokens, credentials or personal email contents.
            log.error("No fue posible entregar el correo con el proveedor {}: {}", provider, ex.getClass().getSimpleName());
            throw new EntregaCorreoException();
        }
    }
}
