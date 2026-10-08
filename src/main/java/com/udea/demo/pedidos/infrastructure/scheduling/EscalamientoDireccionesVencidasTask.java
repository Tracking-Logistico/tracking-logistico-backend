package com.udea.demo.pedidos.infrastructure.scheduling;

import com.udea.demo.pedidos.application.service.VerificacionDireccionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Devuelve al remitente los envíos cuya dirección no fue confirmada dentro del plazo. */
@Component
public class EscalamientoDireccionesVencidasTask {
    private static final Logger log = LoggerFactory.getLogger(EscalamientoDireccionesVencidasTask.class);
    private final VerificacionDireccionService verificacion;

    public EscalamientoDireccionesVencidasTask(VerificacionDireccionService verificacion) {
        this.verificacion = verificacion;
    }

    @Scheduled(cron = "${app.incidencias.escalamiento-cron:0 0 * * * *}")
    public void escalar() {
        LocalDateTime ahora = LocalDateTime.now();
        for (Long pedidoId : verificacion.pendientesDeEscalar(ahora)) {
            try {
                if (verificacion.escalarVencida(pedidoId, ahora)) {
                    log.info("Envío {} escalado a devolución por plazo de verificación de dirección vencido", pedidoId);
                }
            } catch (RuntimeException ex) {
                // Un fallo aislado no detiene el escalamiento de los demás envíos; se reintenta en la próxima ejecución.
                log.error("No fue posible escalar a devolución el envío {}", pedidoId, ex);
            }
        }
    }
}
