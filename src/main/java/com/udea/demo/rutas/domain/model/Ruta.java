package com.udea.demo.rutas.domain.model;

import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.OrdenInvalidoException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Information Expert: la ruta del día de un conductor protege el armado y reordenamiento de sus paradas
@Entity
@Table(name = "rutas")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Long id;

    @Column(name = "id_conductor", nullable = false)
    private Long conductorId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Builder.Default
    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParadaRuta> paradas = new ArrayList<>();

    // Factory method (GRASP Creator): abre la ruta del día para un conductor
    public static Ruta crear(Long conductorId, LocalDate fecha) {
        return Ruta.builder()
                .conductorId(conductorId)
                .fecha(fecha)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    public ParadaRuta agregarParada(Long pedidoId) {
        int siguienteOrden = (int) paradas.stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .count() + 1;

        ParadaRuta parada = ParadaRuta.asignar(this, pedidoId, siguienteOrden);
        paradas.add(parada);
        return parada;
    }

    public void cancelarParada(Long pedidoId) {
        ParadaRuta parada = paradas.stream()
                .filter(p -> p.getPedidoId().equals(pedidoId) && p.getEstado() == EstadoParada.PENDIENTE)
                .findFirst()
                .orElseThrow(() -> new EnvioNoAsignadoException(pedidoId));

        parada.cancelar();
    }

    // Organización de la ruta: aplica el orden indicado por el operador a las paradas activas
    public void reordenar(List<Long> pedidoIdsEnOrden) {
        List<ParadaRuta> activas = paradas.stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .toList();

        Set<Long> pedidosActivos = activas.stream().map(ParadaRuta::getPedidoId).collect(Collectors.toSet());

        if (pedidosActivos.size() != pedidoIdsEnOrden.size() || !pedidosActivos.equals(new HashSet<>(pedidoIdsEnOrden))) {
            throw new OrdenInvalidoException();
        }

        for (int i = 0; i < pedidoIdsEnOrden.size(); i++) {
            Long pedidoId = pedidoIdsEnOrden.get(i);
            int ordenFinal = i + 1;
            activas.stream()
                    .filter(p -> p.getPedidoId().equals(pedidoId))
                    .findFirst()
                    .ifPresent(p -> p.actualizarOrden(ordenFinal));
        }
    }
}
