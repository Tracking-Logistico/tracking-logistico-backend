package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.RangoReprogramacionResponseDTO;
import com.udea.demo.pedidos.application.dto.ReprogramarEntregaRequestDTO;
import com.udea.demo.pedidos.application.dto.SeguimientoClienteResponseDTO;
import com.udea.demo.pedidos.domain.event.EntregaReprogramadaEvent;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.RangoReprogramacion;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** El cliente elige una nueva fecha para una entrega fallida dentro del plazo de retención en bodega. */
@Service
public class ReprogramacionEntregaService {
    private static final String CAMPO_REPROGRAMACION = "REPROGRAMACION";

    private final AccesoClientePedido acceso;
    private final PedidoRepository pedidos;
    private final RegistroHistorialPedido historial;
    private final SeguimientoClienteService seguimiento;
    private final ActorAuthorizationService actores;
    private final ApplicationEventPublisher eventos;
    private final int diasRetencionBodega;

    public ReprogramacionEntregaService(AccesoClientePedido acceso, PedidoRepository pedidos,
                                        RegistroHistorialPedido historial, SeguimientoClienteService seguimiento,
                                        ActorAuthorizationService actores, ApplicationEventPublisher eventos,
                                        @Value("${app.shipment.retencion-bodega-dias:7}") int diasRetencionBodega) {
        this.acceso = acceso;
        this.pedidos = pedidos;
        this.historial = historial;
        this.seguimiento = seguimiento;
        this.actores = actores;
        this.eventos = eventos;
        this.diasRetencionBodega = diasRetencionBodega;
    }

    @Transactional(readOnly = true)
    public RangoReprogramacionResponseDTO rangoDisponible(String numeroTracking) {
        RangoReprogramacion rango = acceso.pedidoDelClienteActual(numeroTracking)
                .rangoReprogramacion(LocalDate.now(), diasRetencionBodega);
        return new RangoReprogramacionResponseDTO(rango.desde(), rango.hasta());
    }

    @Transactional
    public SeguimientoClienteResponseDTO reprogramar(String numeroTracking, ReprogramarEntregaRequestDTO dto) {
        Pedido pedido = acceso.pedidoDelClienteActualParaModificar(numeroTracking);
        pedido.reprogramarEntrega(dto.fecha(), LocalDate.now(), diasRetencionBodega);
        pedidos.saveAndFlush(pedido);
        historial.registrar(pedido.getId(), actores.actorActual().getId(), RegistroHistorialPedido.ESTADO_LOGISTICO,
                CAMPO_REPROGRAMACION, pedido.getEstado().name(), LocalDateTime.now());
        eventos.publishEvent(EntregaReprogramadaEvent.de(pedido));
        return seguimiento.construir(pedido);
    }
}
