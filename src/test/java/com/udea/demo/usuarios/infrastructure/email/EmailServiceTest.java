package com.udea.demo.usuarios.infrastructure.email;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    @Test void sinApiKeyNoRevierteElProceso() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService servicio = new EmailService(sender);
        ReflectionTestUtils.setField(servicio, "mailEnabled", true);
        ReflectionTestUtils.setField(servicio, "provider", "resend");
        ReflectionTestUtils.setField(servicio, "resendApiKey", "");
        ReflectionTestUtils.setField(servicio, "from", "");
        assertThatCode(() -> servicio.enviarVerificacion("test@example.com", "Test", "http://localhost/verificar?token=abc"))
                .doesNotThrowAnyException();
        verifyNoInteractions(sender);
    }

    @Test void falloDeSmtpNoRevierteElProceso() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("servidor sin conexión")).when(sender).send(any(org.springframework.mail.SimpleMailMessage.class));
        EmailService servicio = new EmailService(sender);
        ReflectionTestUtils.setField(servicio, "mailEnabled", true);
        ReflectionTestUtils.setField(servicio, "provider", "smtp");
        ReflectionTestUtils.setField(servicio, "from", "no-reply@example.com");
        assertThatCode(() -> servicio.enviarEnlaceRestablecimiento("test@example.com", "http://localhost/reset/abc"))
                .doesNotThrowAnyException();
    }

    @Test void proveedorInvalidoNoRevierteElProceso() {
        EmailService servicio = new EmailService(mock(JavaMailSender.class));
        ReflectionTestUtils.setField(servicio, "mailEnabled", true);
        ReflectionTestUtils.setField(servicio, "provider", "desconocido");
        assertThatCode(() -> servicio.enviarEnlaceRestablecimiento("test@example.com", "http://localhost/reset/abc"))
                .doesNotThrowAnyException();
    }
}
