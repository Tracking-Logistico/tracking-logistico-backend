package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Rol;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;

/** Resuelve un envío por tracking verificando que pertenece al cliente autenticado (remitente o destinatario). */
@Component
public class AccesoClientePedido {
    private static final List<EstadoPedido> ESTADOS_NO_VISIBLES_DESTINATARIO =
            List.of(EstadoPedido.SOLICITADO, EstadoPedido.CORRECCION_SOLICITADA, EstadoPedido.RECHAZADO);

    private final PedidoRepository pedidos;
    private final ActorAuthorizationService actores;

    public AccesoClientePedido(PedidoRepository pedidos, ActorAuthorizationService actores) {
        this.pedidos = pedidos;
        this.actores = actores;
    }

    public Pedido pedidoDelClienteActual(String numeroTracking) {
        return autorizar(pedidos.findByNumeroTracking(numeroTracking)
                .orElseThrow(() -> new PedidoNoEncontradoException(numeroTracking)));
    }

    /** Igual que {@link #pedidoDelClienteActual} pero bloqueando el envío para modificarlo. */
    public Pedido pedidoDelClienteActualParaModificar(String numeroTracking) {
        return autorizar(pedidos.findByNumeroTrackingForUpdate(numeroTracking)
                .orElseThrow(() -> new PedidoNoEncontradoException(numeroTracking)));
    }

    private Pedido autorizar(Pedido p) {
        var actor = actores.actorActual();
        if (actor.getRol() != Rol.CLIENTE) {
            throw new AccessDeniedException("El seguimiento de cuenta solo está disponible para clientes");
        }
        boolean remitente = p.getClienteId().equals(actores.clienteActualId());
        boolean destinatario = p.getDestinatarioEmail() != null
                && p.getDestinatarioEmail().equalsIgnoreCase(actor.getEmail())
                && !ESTADOS_NO_VISIBLES_DESTINATARIO.contains(p.getEstado());
        if (!remitente && !destinatario) {
            throw new AccessDeniedException("El pedido no está asociado al cliente autenticado");
        }
        return p;
    }
}
