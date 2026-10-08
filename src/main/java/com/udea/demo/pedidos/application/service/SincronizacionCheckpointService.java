package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.ItemCheckpointOfflineDTO;
import com.udea.demo.pedidos.application.dto.RegistroCheckpointResultadoDTO;
import com.udea.demo.pedidos.application.dto.SincronizacionCheckpointsResponseDTO;
import com.udea.demo.pedidos.application.dto.SincronizacionCheckpointsResponseDTO.EstadoSincronizacion;
import com.udea.demo.pedidos.application.dto.SincronizacionCheckpointsResponseDTO.ResultadoItem;
import com.udea.demo.pedidos.application.dto.SincronizarCheckpointsRequestDTO;
import com.udea.demo.pedidos.domain.model.EstadoRegistroCheckpoint;
import com.udea.demo.pedidos.interfaces.persistence.CheckpointPedidoRepository;
import com.udea.demo.pedidos.interfaces.services.CheckpointServiceI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Sincroniza los checkpoints capturados sin conexión respetando el orden cronológico real del dispositivo.
 * Cada evento se procesa en su propia transacción: un evento rechazado no revierte los demás.
 */
@Service
public class SincronizacionCheckpointService {
    private static final Logger log = LoggerFactory.getLogger(SincronizacionCheckpointService.class);
    private final CheckpointServiceI checkpoints;
    private final CheckpointPedidoRepository registros;

    public SincronizacionCheckpointService(CheckpointServiceI checkpoints, CheckpointPedidoRepository registros) {
        this.checkpoints = checkpoints;
        this.registros = registros;
    }

    public SincronizacionCheckpointsResponseDTO sincronizar(SincronizarCheckpointsRequestDTO dto) {
        List<ResultadoItem> resultados = dto.eventos().stream()
                .sorted(Comparator.comparing(item -> item.checkpoint().fechaDispositivo()))
                .map(this::procesar)
                .toList();
        return SincronizacionCheckpointsResponseDTO.de(resultados);
    }

    private ResultadoItem procesar(ItemCheckpointOfflineDTO item) {
        String idEvento = item.checkpoint().idEventoCliente();
        try {
            RegistroCheckpointResultadoDTO r = checkpoints.registrarSincronizado(item.pedidoId(), item.checkpoint());
            return new ResultadoItem(idEvento, item.pedidoId(), estado(r), r.checkpoint().id(),
                    r.checkpoint().motivoRevision());
        } catch (DataIntegrityViolationException ex) {
            // Otro envío concurrente del mismo evento ganó la inserción: es un duplicado, no un error.
            return registros.findByIdEventoCliente(idEvento)
                    .map(c -> new ResultadoItem(idEvento, item.pedidoId(), EstadoSincronizacion.DUPLICADO, c.getId(), null))
                    .orElseGet(() -> rechazado(item, "El evento entra en conflicto con otro registro"));
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Checkpoint offline rechazado idEvento={} pedido={}: {}", idEvento, item.pedidoId(), ex.getMessage());
            return rechazado(item, ex.getMessage());
        }
    }

    private static EstadoSincronizacion estado(RegistroCheckpointResultadoDTO r) {
        if (r.duplicado()) return EstadoSincronizacion.DUPLICADO;
        return r.checkpoint().estadoRegistro() == EstadoRegistroCheckpoint.PENDIENTE_REVISION
                ? EstadoSincronizacion.PENDIENTE_REVISION : EstadoSincronizacion.APLICADO;
    }

    private static ResultadoItem rechazado(ItemCheckpointOfflineDTO item, String motivo) {
        return new ResultadoItem(item.checkpoint().idEventoCliente(), item.pedidoId(),
                EstadoSincronizacion.RECHAZADO, null, motivo);
    }
}
