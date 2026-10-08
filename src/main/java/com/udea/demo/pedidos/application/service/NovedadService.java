package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.NovedadResponseDTO;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Panel de "novedades" del operador: movimientos logísticos recientes de todos los envíos. */
@Service
public class NovedadService {
    static final List<String> TIPOS_NOVEDAD = List.of(RegistroHistorialPedido.CHECKPOINT,
            RegistroHistorialPedido.CHECKPOINT_EN_REVISION, RegistroHistorialPedido.ESTADO_LOGISTICO,
            RegistroHistorialPedido.INCIDENCIA);

    private final HistorialPedidoRepository historial;
    private final ActorAuthorizationService actores;

    public NovedadService(HistorialPedidoRepository historial, ActorAuthorizationService actores) {
        this.historial = historial;
        this.actores = actores;
    }

    @Transactional(readOnly = true)
    public Page<NovedadResponseDTO> listar(Pageable pageable) {
        actores.operadorActualId();
        return historial.findByTipoEventoInOrderByFechaDescIdDesc(TIPOS_NOVEDAD, pageable)
                .map(h -> new NovedadResponseDTO(h.getId(), h.getPedidoId(), h.getUsuarioId(), h.getTipoEvento(),
                        h.getDetalle(), h.getFecha()));
    }
}
