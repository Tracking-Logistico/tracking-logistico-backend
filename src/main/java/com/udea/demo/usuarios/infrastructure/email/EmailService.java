package com.udea.demo.usuarios.infrastructure.email;

import com.udea.demo.usuarios.interfaces.services.EmailServiceI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Adaptador de correo best-effort: errores de proveedores no alteran los casos de uso. */
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
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return factory;
    }

    @Value("${app.mail.enabled:true}") private boolean mailEnabled;
    @Value("${app.mail.provider:resend}") private String provider;
    @Value("${app.mail.resend-api-key:}") private String resendApiKey;
    @Value("${app.mail.from:}") private String from;
    @Value("${spring.mail.username:}") private String smtpUsername;
    @Value("${app.mail.log-action-links:false}") private boolean logActionLinks;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarVerificacion(String destinatario, String nombreUsuario, String enlace) {
        enviar("VERIFICACION", destinatario, "Verificación de cuenta - Tracking Logístico",
                "Hola " + nombreUsuario + ",\n\n" +
                "Para activar tu cuenta, ingresa a este enlace: \n" + enlace +
                "\n\nEl enlace expira en 2 horas.", enlace);
    }

    @Override
    public void enviarEnlaceRestablecimiento(String destinatario, String enlace) {
        enviar("RESTABLECIMIENTO", destinatario, "Restablecimiento de contraseña",
                "Se solicitó restablecer tu contraseña.\n\nUsa el siguiente enlace dentro de 45 minutos: \n" +
                enlace + "\n\nSi no solicitaste el cambio, ignora este mensaje.", enlace);
    }

    private void enviar(String evento, String destinatario, String asunto, String contenido, String enlace) {
        if (!mailEnabled) {
            advertir(evento, destinatario, "MAIL_ENABLED=false; habilitar correo para realizar entregas", enlace);
            return;
        }
        try {
            if ("resend".equalsIgnoreCase(provider)) {
                if (resendApiKey == null || resendApiKey.isBlank() || from == null || from.isBlank()) {
                    advertir(evento, destinatario,
                            "Resend sin RESEND_API_KEY o MAIL_FROM; agregar ambos en Render (dominio remitente verificado)", enlace);
                    return;
                }
                resend.post().uri("/emails")
                        .header("Authorization", "Bearer " + resendApiKey)
                        .body(Map.of("from", from, "to", List.of(destinatario), "subject", asunto, "text", contenido))
                        .retrieve().toBodilessEntity();
            } else if ("smtp".equalsIgnoreCase(provider)) {
                String remitente = from == null || from.isBlank() ? smtpUsername : from;
                if (remitente == null || remitente.isBlank()) {
                    advertir(evento, destinatario, "SMTP sin MAIL_FROM o MAIL_USERNAME", enlace);
                    return;
                }
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setFrom(remitente);
                mensaje.setTo(destinatario);
                mensaje.setSubject(asunto);
                mensaje.setText(contenido);
                mailSender.send(mensaje);
            } else {
                advertir(evento, destinatario,
                        "MAIL_PROVIDER no soportado; configurar resend (HTTPS) o smtp (solo local/pago)", enlace);
                return;
            }
            log.info("MAIL_SOLICITUD_ACEPTADA evento={} proveedor={} destinatario={}", evento, provider, enmascarar(destinatario));
        } catch (RestClientResponseException ex) {
            advertir(evento, destinatario, "Proveedor HTTP devolvió estado " + ex.getStatusCode().value()
                    + "; revisar API key, dominio verificado, destinatario y cuota de Resend", enlace);
        } catch (RuntimeException ex) {
            advertir(evento, destinatario, "Fallo de red/proveedor tipo=" + ex.getClass().getSimpleName()
                    + "; revisar conectividad, configuración y disponibilidad del proveedor", enlace);
        }
    }

    private void advertir(String evento, String destinatario, String motivo, String enlace) {
        log.warn("MAIL_NO_ENTREGADO evento={} proveedor={} destinatario={} motivo={} resultado=PROCESO_CONTINUA",
                evento, provider, enmascarar(destinatario), motivo);
        if (logActionLinks) {
            // SOLO desarrollo local; deshabilitado por defecto. Los enlaces son credenciales de un solo uso.
            log.warn("MAIL_ENLACE_DESARROLLO evento={} enlace={} (desactivar MAIL_LOG_ACTION_LINKS fuera de local)", evento, enlace);
        }
    }

    private String enmascarar(String email) {
        if (email == null || !email.contains("@")) return "***";
        return (email.length() > 1 ? email.substring(0, 1) : "*") + "***@" + email.substring(email.indexOf('@') + 1);
    }
}
