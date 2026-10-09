package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.model.ResultadoEntrega;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class SincronizacionEntregasServiceTest {
    @Test void procesaLoteEnOrdenYConservaResultadosBuenosYMalos() {
        ResultadoEntregaServiceI resultados = Mockito.mock(ResultadoEntregaServiceI.class);
        SincronizacionEntregasService service = new SincronizacionEntregasService(resultados);
        ResultadoEntregaRequestDTO bueno = evento(ResultadoEntrega.ENTREGADO, 2);
        ResultadoEntregaRequestDTO malo = evento(ResultadoEntrega.ENTREGADO, 1);
        when(resultados.registrar(1L, bueno)).thenReturn(response(bueno, "APLICADO", false));
        when(resultados.registrar(2L, malo)).thenThrow(new IllegalStateException("no está en reparto"));

        var response = service.sincronizar(new SincronizarEntregasRequestDTO(List.of(
                new ResultadoEntregaOfflineDTO(1L, bueno), new ResultadoEntregaOfflineDTO(2L, malo))));

        assertThat(response.resultados()).extracting(SincronizacionEventoResponseDTO::estado)
                .containsExactly("APLICADO", "RECHAZADO");
        assertThat(response.resultados().get(1).motivoRechazo()).isEqualTo("no está en reparto");
    }

    @Test void eventoRepetidoSeDevuelveComoDuplicado() {
        ResultadoEntregaServiceI resultados = Mockito.mock(ResultadoEntregaServiceI.class);
        SincronizacionEntregasService service = new SincronizacionEntregasService(resultados);
        ResultadoEntregaRequestDTO evento = evento(ResultadoEntrega.ENTREGADO, 1);
        when(resultados.registrar(1L, evento)).thenReturn(response(evento, "DUPLICADO", true));
        var response = service.sincronizar(new SincronizarEntregasRequestDTO(
                List.of(new ResultadoEntregaOfflineDTO(1L, evento))));
        assertThat(response.resultados().get(0).estado()).isEqualTo("DUPLICADO");
        assertThat(response.resultados().get(0).motivoRechazo()).isNull();
    }

    private static ResultadoEntregaRequestDTO evento(ResultadoEntrega resultado, int minutesAgo) {
        return new ResultadoEntregaRequestDTO(resultado, null, null, null, null,
                OffsetDateTime.now().minusMinutes(minutesAgo), UUID.randomUUID().toString());
    }
    private static ResultadoEntregaResponseDTO response(ResultadoEntregaRequestDTO dto, String estado, boolean duplicado) {
        return new ResultadoEntregaResponseDTO(dto.idEventoCliente(), 1L, dto.resultado(), estado,
                dto.fechaEvento().toLocalDateTime(), duplicado);
    }
}
