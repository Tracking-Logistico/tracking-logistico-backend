package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class SincronizacionEntregasService {
    private final ResultadoEntregaServiceI resultados;

    public SincronizacionEntregasService(ResultadoEntregaServiceI resultados) {
        this.resultados = resultados;
    }

    public SincronizacionEntregasResponseDTO sincronizar(SincronizarEntregasRequestDTO dto) {
        var respuestas = dto.eventos().stream()
                .sorted(Comparator.comparing(item -> item.evento().fechaEvento()))
                .map(this::procesar)
                .toList();
        return new SincronizacionEntregasResponseDTO(respuestas);
    }

    private SincronizacionEventoResponseDTO procesar(ResultadoEntregaOfflineDTO item) {
        try {
            ResultadoEntregaResponseDTO response = resultados.registrar(item.pedidoId(), item.evento());
            return new SincronizacionEventoResponseDTO(response.idEventoCliente(),
                    response.duplicado() ? "DUPLICADO" : "APLICADO", null);
        } catch (DataIntegrityViolationException ex) {
            return new SincronizacionEventoResponseDTO(item.evento().idEventoCliente(), "DUPLICADO", null);
        } catch (RuntimeException ex) {
            return new SincronizacionEventoResponseDTO(item.evento().idEventoCliente(), "RECHAZADO", ex.getMessage());
        }
    }
}
