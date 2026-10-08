package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.ConfirmarDireccionRequestDTO;
import com.udea.demo.pedidos.application.dto.SeguimientoClienteResponseDTO;
import com.udea.demo.pedidos.domain.event.DevolucionIniciadaEvent;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.MotivoDevolucion;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Gestión de "Dirección por verificar": respuesta del cliente y escalamiento a devolución al vencer el plazo. */
@Service
public class VerificacionDireccionService {
    private static final String CAMPO_DIRECCION_CONFIRMADA = "DIRECCION_CONFIRMADA";
    private static final String CAMPO_PLAZO_VENCIDO = "PLAZO_DIRECCION_VENCIDO";

    private final AccesoClientePedido acceso;
    private final PedidoRepository pedidos;
    private final RegistroHistorialPedido historial;
    private final SeguimientoClienteService seguimiento;
    private final ActorAuthorizationService actores;
    private final ApplicationEventPublisher eventos;

    public VerificacionDireccionService(AccesoClientePedido acceso, PedidoRepository pedidos,
                                        RegistroHistorialPedido historial, SeguimientoClienteService seguimiento,
                                        ActorAuthorizationService actores, ApplicationEventPublisher eventos) {
        this.acceso = acceso;
        this.pedidos = pedidos;
        this.historial = historial;
        this.seguimiento = seguimiento;
        this.actores = actores;
        this.eventos = eventos;
    }

    @Transactional
    public SeguimientoClienteResponseDTO confirmar(String numeroTracking, ConfirmarDireccionRequestDTO dto) {
        Pedido pedido = acceso.pedidoDelClienteActualParaModificar(numeroTracking);
        LocalDateTime ahora = LocalDateTime.now();
        pedido.confirmarDireccion(dto.direccionDestino(), dto.ciudadDestino(), dto.codigoPostalDestino(), ahora);
        pedidos.saveAndFlush(pedido);
        historial.registrar(pedido.getId(), actores.actorActual().getId(), RegistroHistorialPedido.ESTADO_LOGISTICO,
                CAMPO_DIRECCION_CONFIRMADA, pedido.getEstado().name(), ahora);
        return seguimiento.construir(pedido);
    }

    @Transactional(readOnly = true)
    public List<Long> pendientesDeEscalar(LocalDateTime ahora) {
        return pedidos.findIdsConPlazoDireccionVencido(EstadoPedido.DIRECCION_POR_VERIFICAR, ahora);
    }

    /** Escala un envío vencido a devolución; se re-verifica bajo bloqueo por si el cliente respondió a tiempo. */
    @Transactional
    public boolean escalarVencida(Long pedidoId, LocalDateTime ahora) {
        Pedido pedido = pedidos.findByIdForUpdate(pedidoId).orElse(null);
        if (pedido == null) return false;
        EstadoPedido anterior = pedido.getEstado();
        if (!pedido.devolverPorPlazoVencido(ahora)) return false;
        pedidos.saveAndFlush(pedido);
        historial.registrar(pedidoId, null, RegistroHistorialPedido.ESTADO_LOGISTICO, CAMPO_PLAZO_VENCIDO,
                pedido.getEstado().name(), ahora);
        eventos.publishEvent(DevolucionIniciadaEvent.de(pedido, anterior, MotivoDevolucion.PLAZO_DIRECCION_VENCIDO));
        return true;
    }
}
