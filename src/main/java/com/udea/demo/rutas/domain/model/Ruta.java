package com.udea.demo.rutas.domain.model;

import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.OrdenInvalidoException;
import jakarta.persistence.*;

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

    @Column(name = "orden_manual", nullable = false)
    private boolean ordenManual = false;

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
        int siguienteOrden = paradas.stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .mapToInt(ParadaRuta::getOrden).max().orElse(0) + 1;

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
        List<ParadaRuta> activas = paradas.stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .sorted(java.util.Comparator.comparing(ParadaRuta::getOrden))
                .toList();
        for (int i = 0; i < activas.size(); i++) activas.get(i).actualizarOrden(i + 1);
    }

    // Organización de la ruta: aplica el orden indicado por el operador a las paradas activas
    public void reordenar(List<Long> pedidoIdsEnOrden) {
        aplicarOrden(pedidoIdsEnOrden);
        this.ordenManual = true;
    }

    public void reordenarAutomatico(List<Long> pedidoIdsEnOrden) {
        aplicarOrden(pedidoIdsEnOrden);
    }

    private void aplicarOrden(List<Long> pedidoIdsEnOrden) {
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


    public Ruta() {}
    public Ruta(Long id, Long conductorId, LocalDate fecha, LocalDateTime fechaCreacion, boolean ordenManual, List<ParadaRuta> paradas) {
        this.id = id;
        this.conductorId = conductorId;
        this.fecha = fecha;
        this.fechaCreacion = fechaCreacion;
        this.ordenManual = ordenManual;
        this.paradas = paradas;
    }

    public Long getId() { return id; }
    public Long getConductorId() { return conductorId; }
    public LocalDate getFecha() { return fecha; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public boolean isOrdenManual() { return ordenManual; }
    public List<ParadaRuta> getParadas() { return paradas; }

    public static RutaBuilder builder() { return new RutaBuilder(); }
    public static class RutaBuilder {
        private Long id;
        private Long conductorId;
        private LocalDate fecha;
        private LocalDateTime fechaCreacion;
        private boolean ordenManual;
        private boolean ordenManualSet;
        private List<ParadaRuta> paradas;
        private boolean paradasSet;

        public RutaBuilder id(Long id) { this.id = id; return this; }
        public RutaBuilder conductorId(Long conductorId) { this.conductorId = conductorId; return this; }
        public RutaBuilder fecha(LocalDate fecha) { this.fecha = fecha; return this; }
        public RutaBuilder fechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; return this; }
        public RutaBuilder ordenManual(boolean ordenManual) { this.ordenManual = ordenManual; this.ordenManualSet = true; return this; }
        public RutaBuilder paradas(List<ParadaRuta> paradas) { this.paradas = paradas; this.paradasSet = true; return this; }
        public Ruta build() {
            boolean ordenManualValue = ordenManualSet ? ordenManual : false;
            List<ParadaRuta> paradasValue = paradasSet ? paradas : new ArrayList<>();
            return new Ruta(id, conductorId, fecha, fechaCreacion, ordenManualValue, paradasValue);
        }
    }
}
