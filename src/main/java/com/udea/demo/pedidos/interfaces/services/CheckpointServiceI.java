package com.udea.demo.pedidos.interfaces.services;

import com.udea.demo.pedidos.application.dto.CheckpointResponseDTO;
import com.udea.demo.pedidos.application.dto.CheckpointsPendientesRevisionDTO;
import com.udea.demo.pedidos.application.dto.RegistrarCheckpointRequestDTO;
import com.udea.demo.pedidos.application.dto.RegistroCheckpointResultadoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CheckpointServiceI {
    /** Checkpoint en línea: cualquier conflicto con el estado del envío se rechaza. */
    RegistroCheckpointResultadoDTO registrar(Long pedidoId, RegistrarCheckpointRequestDTO dto);

    /** Checkpoint capturado sin conexión: un conflicto con el estado actual queda pendiente de revisión. */
    RegistroCheckpointResultadoDTO registrarSincronizado(Long pedidoId, RegistrarCheckpointRequestDTO dto);

    CheckpointsPendientesRevisionDTO misPendientesRevision();

    Page<CheckpointResponseDTO> pendientesRevision(Pageable pageable);
}
