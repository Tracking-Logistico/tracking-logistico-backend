package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.TipoIncidencia;

public record TipoIncidenciaResponseDTO(String codigo, String descripcion, boolean requiereComentario,
                                        EstadoPedido estadoResultante, boolean cuentaComoIntento) {
    public static TipoIncidenciaResponseDTO de(TipoIncidencia t) {
        return new TipoIncidenciaResponseDTO(t.name(), t.descripcion(), t.requiereComentario(), t.estadoResultante(),
                t.cuentaComoIntento());
    }
}
