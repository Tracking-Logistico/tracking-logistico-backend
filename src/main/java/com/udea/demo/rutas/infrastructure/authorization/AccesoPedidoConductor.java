package com.udea.demo.rutas.infrastructure.authorization;
import com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import org.springframework.stereotype.Component;
@Component
public class AccesoPedidoConductor implements AccesoPedidoConductorI {
    private final ParadaRutaRepository paradas;
    public AccesoPedidoConductor(ParadaRutaRepository paradas) { this.paradas = paradas; }
    @Override public boolean tieneAsignacionActiva(Long pedidoId, Long usuarioId) {
        return paradas.existsByPedidoIdAndRutaConductorUsuarioIdAndEstado(pedidoId, usuarioId, EstadoParada.PENDIENTE);
    }
}
