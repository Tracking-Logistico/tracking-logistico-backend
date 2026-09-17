package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.TrackingNoActivoException;
import com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Information Expert: el propio Pedido conoce y protege las reglas de su ciclo de vida
@Entity
@Table(name = "pedidos")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @Column(name = "numero_pedido", nullable = false, unique = true)
    private String numeroPedido;

    @Column(name = "id_cliente", nullable = false)
    private Long clienteId;

    @Column(name = "direccion_origen", nullable = false)
    private String direccionOrigen;

    @Column(name = "direccion_destino", nullable = false)
    private String direccionDestino;

    @Column(name = "descripcion_paquete", nullable = false)
    private String descripcionPaquete;

    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    @Column(name = "largo_cm", nullable = false)
    private Double largoCm;

    @Column(name = "ancho_cm", nullable = false)
    private Double anchoCm;

    @Column(name = "alto_cm", nullable = false)
    private Double altoCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_servicio", nullable = false)
    private TipoServicio tipoServicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad_sugerida", nullable = false)
    private Prioridad prioridadSugerida;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad_confirmada")
    private Prioridad prioridadConfirmada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPedido estado;

    @Column(name = "observaciones_validacion", length = 500)
    private String observacionesValidacion;

    @Column(name = "id_operador_validador")
    private Long operadorValidadorId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "numero_tracking", unique = true)
    private String numeroTracking;

    @Column(name = "fecha_activacion_tracking")
    private LocalDateTime fechaActivacionTracking;

    @Builder.Default
    @Column(name = "etiqueta_impresa", nullable = false)
    private Boolean etiquetaImpresa = false;

    @Column(name = "fecha_impresion_etiqueta")
    private LocalDateTime fechaImpresionEtiqueta;

    // Factory method (GRASP Creator): agrupa los datos necesarios para nacer en estado RECIBIDO
    public static Pedido recibir(Long clienteId, String direccionOrigen, String direccionDestino,
                                  String descripcionPaquete, Double pesoKg, Double largoCm,
                                  Double anchoCm, Double altoCm, TipoServicio tipoServicio,
                                  String numeroPedido, Prioridad prioridadSugerida) {
        return Pedido.builder()
                .numeroPedido(numeroPedido)
                .clienteId(clienteId)
                .direccionOrigen(direccionOrigen)
                .direccionDestino(direccionDestino)
                .descripcionPaquete(descripcionPaquete)
                .pesoKg(pesoKg)
                .largoCm(largoCm)
                .anchoCm(anchoCm)
                .altoCm(altoCm)
                .tipoServicio(tipoServicio)
                .prioridadSugerida(prioridadSugerida)
                .estado(EstadoPedido.RECIBIDO)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    // Confirma o ajusta la prioridad sugerida y aprueba/rechaza el pedido
    public void validar(boolean aprobar, Prioridad prioridadConfirmadaSolicitada,
                         String observaciones, Long operadorId) {
        EstadoPedido destino = aprobar ? EstadoPedido.VALIDADO : EstadoPedido.RECHAZADO;

        if (!this.estado.puedeTransicionarA(destino)) {
            throw new TransicionEstadoInvalidaException(this.estado, destino);
        }

        this.prioridadConfirmada = prioridadConfirmadaSolicitada != null
                ? prioridadConfirmadaSolicitada
                : this.prioridadSugerida;
        this.estado = destino;
        this.observacionesValidacion = observaciones;
        this.operadorValidadorId = operadorId;
        this.fechaValidacion = LocalDateTime.now();
    }

    // Criterio: Activación de Tracking (solo procede si el pedido ya fue validado)
    public void activarTracking(String numeroTracking) {
        EstadoPedido destino = EstadoPedido.EN_TRANSITO;

        if (!this.estado.puedeTransicionarA(destino)) {
            throw new TransicionEstadoInvalidaException(this.estado, destino);
        }

        this.numeroTracking = numeroTracking;
        this.fechaActivacionTracking = LocalDateTime.now();
        this.estado = destino;
    }

    // Criterio: Generación e Impresión de Etiqueta (exige tracking activo)
    public void confirmarImpresionEtiqueta() {
        if (this.numeroTracking == null) {
            throw new TrackingNoActivoException(this.id);
        }

        this.etiquetaImpresa = true;
        this.fechaImpresionEtiqueta = LocalDateTime.now();
    }
}
