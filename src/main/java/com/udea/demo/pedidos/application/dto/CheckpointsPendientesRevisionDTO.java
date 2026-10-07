package com.udea.demo.pedidos.application.dto;

import java.util.List;

public record CheckpointsPendientesRevisionDTO(int total, List<CheckpointResponseDTO> checkpoints) {}
