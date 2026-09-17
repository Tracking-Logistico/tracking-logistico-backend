package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import com.udea.demo.rutas.application.dto.AsignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ParadaResponseDTO;
import com.udea.demo.rutas.application.dto.ReasignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReordenarRutaRequestDTO;
import com.udea.demo.rutas.application.dto.RutaResponseDTO;
import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.EnvioYaAsignadoException;
import com.udea.demo.rutas.domain.exception.RutaNoEncontradaException;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.ParadaRuta;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.rutas.interfaces.services.RutaServiceI;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// Facade hacia el módulo rutas; colabora con pedidos solo a través de su puerto público (bajo acoplamiento)
@Service
public class RutaService implements RutaServiceI {

    private final RutaRepository rutaRepository;
    private final ParadaRutaRepository paradaRutaRepository;
    private final GestorRutaActiva gestorRutaActiva;
    private final PedidoServiceI pedidoServiceI;

    public RutaService(RutaRepository rutaRepository,
                        ParadaRutaRepository paradaRutaRepository,
                        GestorRutaActiva gestorRutaActiva,
                        PedidoServiceI pedidoServiceI) {
        this.rutaRepository = rutaRepository;
        this.paradaRutaRepository = paradaRutaRepository;
        this.gestorRutaActiva = gestorRutaActiva;
        this.pedidoServiceI = pedidoServiceI;
    }

    @Override
    public List<PedidoResponseDTO> listarEnviosPendientesDeAsignacion() {
        List<Long> asignados = paradaRutaRepository.findPedidoIdsByEstado(EstadoParada.PENDIENTE);

        return pedidoServiceI.listarEnTransito().stream()
                .filter(pedido -> !asignados.contains(pedido.id()))
                .toList();
    }

    @Override
    @Transactional
    public RutaResponseDTO asignarEnvio(AsignarEnvioRequestDTO dto) {
        paradaRutaRepository.findByPedidoIdAndEstado(dto.pedidoId(), EstadoParada.PENDIENTE)
                .ifPresent(p -> {
                    throw new EnvioYaAsignadoException(dto.pedidoId());
                });

        Ruta ruta = gestorRutaActiva.obtenerOCrear(dto.conductorId());
        ruta.agregarParada(dto.pedidoId());

        return mapToDTO(rutaRepository.save(ruta));
    }

    @Override
    @Transactional
    public RutaResponseDTO reordenarRuta(Long rutaId, ReordenarRutaRequestDTO dto) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> RutaNoEncontradaException.porId(rutaId));

        ruta.reordenar(dto.pedidoIdsEnOrden());

        return mapToDTO(rutaRepository.save(ruta));
    }

    @Override
    @Transactional
    public RutaResponseDTO reasignarEnvio(ReasignarEnvioRequestDTO dto) {
        ParadaRuta paradaActual = paradaRutaRepository.findByPedidoIdAndEstado(dto.pedidoId(), EstadoParada.PENDIENTE)
                .orElseThrow(() -> new EnvioNoAsignadoException(dto.pedidoId()));

        Ruta rutaOrigen = paradaActual.getRuta();
        rutaOrigen.cancelarParada(dto.pedidoId());
        rutaRepository.save(rutaOrigen);

        Ruta rutaDestino = gestorRutaActiva.obtenerOCrear(dto.nuevoConductorId());
        rutaDestino.agregarParada(dto.pedidoId());

        return mapToDTO(rutaRepository.save(rutaDestino));
    }

    @Override
    public RutaResponseDTO obtenerRutaActivaDeConductor(Long conductorId) {
        Ruta ruta = rutaRepository.findByConductorIdAndFecha(conductorId, LocalDate.now())
                .orElseThrow(() -> RutaNoEncontradaException.paraConductor(conductorId));

        return mapToDTO(ruta);
    }

    private RutaResponseDTO mapToDTO(Ruta ruta) {
        List<ParadaResponseDTO> paradas = ruta.getParadas().stream()
                .sorted((a, b) -> a.getOrden().compareTo(b.getOrden()))
                .map(p -> new ParadaResponseDTO(
                        p.getId(),
                        p.getPedidoId(),
                        p.getOrden(),
                        p.getEstado().name(),
                        p.getFechaAsignacion()
                ))
                .toList();

        return new RutaResponseDTO(
                ruta.getId(),
                ruta.getConductorId(),
                ruta.getFecha(),
                paradas
        );
    }
}
