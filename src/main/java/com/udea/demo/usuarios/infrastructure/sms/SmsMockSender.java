package com.udea.demo.usuarios.infrastructure.sms;

import com.udea.demo.usuarios.interfaces.services.SmsSenderI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación mock de SMS para desarrollo. Loguea el mensaje y lo da por enviado.
 * Se activa cuando app.notifications.sms-provider=mock (por defecto).
 *
 * Para integrar Twilio en el futuro: crear TwilioSmsSender con
 * @ConditionalOnProperty(name = "app.notifications.sms-provider", havingValue = "twilio")
 * y añadir la dependencia de Twilio al pom.xml. El resto del código no cambia.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.sms-provider", havingValue = "mock", matchIfMissing = true)
public class SmsMockSender implements SmsSenderI {

    private static final Logger log = LoggerFactory.getLogger(SmsMockSender.class);

    @Value("${app.notifications.sms-enabled:false}")
    private boolean smsEnabled;

    @Override
    public boolean enviar(String telefono, String mensaje) {
        if (!smsEnabled) {
            log.info("SMS_MOCK_DESHABILITADO telefono={} mensaje={}", enmascarar(telefono), mensaje);
            return true;
        }
        log.info("SMS_MOCK_ENVIADO telefono={} mensaje={}", enmascarar(telefono), mensaje);
        return true;
    }

    private String enmascarar(String telefono) {
        if (telefono == null || telefono.length() < 4) return "***";
        return "***" + telefono.substring(telefono.length() - 4);
    }
}
