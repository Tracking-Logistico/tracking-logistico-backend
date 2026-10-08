package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccesoClientePedido - pertenencia del envío al cliente")
class AccesoClientePedidoTest {
    @Mock private PedidoRepository pedidos;
    @Mock private ActorAuthorizationService actores;
    @InjectMocks private AccesoClientePedido acceso;

    private Pedido pedido(EstadoPedido estado) {
        return Pedido.builder().id(1L).clienteId(10L).numeroTracking("LT1").destinatarioEmail("dest@test.com")
                .estado(estado).build();
    }

    private void actor(String email, Rol rol, Long clienteId) {
        when(actores.actorActual()).thenReturn(Usuario.builder().id(5L).email(email).rol(rol).build());
        if (rol == Rol.CLIENTE) when(actores.clienteActualId()).thenReturn(clienteId);
    }

    @Test
    @DisplayName("El remitente accede a su envío")
    void remitente() {
        when(pedidos.findByNumeroTrackingForUpdate("LT1")).thenReturn(Optional.of(pedido(EstadoPedido.ENTREGA_FALLIDA)));
        actor("rem@test.com", Rol.CLIENTE, 10L);

        assertThat(acceso.pedidoDelClienteActualParaModificar("LT1").getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("El destinatario registrado accede al envío")
    void destinatario() {
        when(pedidos.findByNumeroTracking("LT1")).thenReturn(Optional.of(pedido(EstadoPedido.EN_REPARTO)));
        actor("DEST@test.com", Rol.CLIENTE, 99L);

        assertThat(acceso.pedidoDelClienteActual("LT1")).isNotNull();
    }

    @Test
    @DisplayName("Un cliente ajeno es rechazado")
    void ajeno() {
        when(pedidos.findByNumeroTracking("LT1")).thenReturn(Optional.of(pedido(EstadoPedido.EN_REPARTO)));
        actor("otro@test.com", Rol.CLIENTE, 99L);

        assertThatThrownBy(() -> acceso.pedidoDelClienteActual("LT1")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Un operador no usa el seguimiento de cuenta del cliente")
    void noCliente() {
        when(pedidos.findByNumeroTracking("LT1")).thenReturn(Optional.of(pedido(EstadoPedido.EN_REPARTO)));
        actor("op@test.com", Rol.OPERADOR, null);

        assertThatThrownBy(() -> acceso.pedidoDelClienteActual("LT1")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Tracking inexistente responde no encontrado")
    void noExiste() {
        when(pedidos.findByNumeroTracking("NO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acceso.pedidoDelClienteActual("NO")).isInstanceOf(PedidoNoEncontradoException.class);
    }
}
