package com.udea.demo.rutas.application.dto;

import java.util.List;

public record RutaConductorResponseDTO(List<ParadaRutaConductorDTO> paradas,
                                       ParadaRutaConductorDTO siguiente) {}
