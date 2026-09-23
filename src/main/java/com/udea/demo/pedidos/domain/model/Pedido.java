package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.TrackingNoActivoException;
import com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido") private Long id;
    @Column(name = "numero_pedido", nullable = false, unique = true) private String numeroPedido;
    @Column(name = "id_cliente", nullable = false) private Long clienteId;
    @Column(name = "direccion_origen", nullable = false) private String direccionOrigen;
    @Column(name = "direccion_destino", nullable = false) private String direccionDestino;
    @Column(name = "descripcion_paquete", nullable = false) private String descripcionPaquete;
    @Column(name = "ciudad_origen", length = 100) private String ciudadOrigen;
    @Column(name = "codigo_postal_origen", length = 12) private String codigoPostalOrigen;
    @Column(name = "ciudad_destino", length = 100) private String ciudadDestino;
    @Column(name = "codigo_postal_destino", length = 12) private String codigoPostalDestino;
    @Column(name = "remitente_nombre", length = 120) private String remitenteNombre;
    @Column(name = "remitente_email", length = 255) private String remitenteEmail;
    @Column(name = "remitente_telefono", length = 20) private String remitenteTelefono;
    @Version @Column(name = "version", nullable = false) @Builder.Default private Long version = 0L;
    @Column(name = "destinatario_nombre", length = 120) private String destinatarioNombre;
    @Column(name = "destinatario_telefono", length = 20) private String destinatarioTelefono;
    @Column(name = "peso_kg", nullable = false) private Double pesoKg;
    @Column(name = "largo_cm", nullable = false) private Double largoCm;
    @Column(name = "ancho_cm", nullable = false) private Double anchoCm;
    @Column(name = "alto_cm", nullable = false) private Double altoCm;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_servicio", nullable = false) private TipoServicio tipoServicio;
    @Enumerated(EnumType.STRING) @Column(name = "prioridad_sugerida", nullable = false) private Prioridad prioridadSugerida;
    @Enumerated(EnumType.STRING) @Column(name = "prioridad_confirmada") private Prioridad prioridadConfirmada;
    @Enumerated(EnumType.STRING) @Column(name = "estado", nullable = false) private EstadoPedido estado;
    @Column(name = "observaciones_validacion", length = 500) private String observacionesValidacion;
    @Column(name = "justificacion_prioridad", length = 500) private String justificacionPrioridad;
    @Column(name = "id_operador_validador") private Long operadorValidadorId;
    @Column(name = "fecha_creacion", nullable = false) private LocalDateTime fechaCreacion;
    @Column(name = "fecha_validacion") private LocalDateTime fechaValidacion;
    @Column(name = "numero_tracking", unique = true) private String numeroTracking;
    @Column(name = "fecha_activacion_tracking") private LocalDateTime fechaActivacionTracking;
    @Builder.Default @Column(name = "etiqueta_impresa", nullable = false) private Boolean etiquetaImpresa = false;
    @Column(name = "fecha_impresion_etiqueta") private LocalDateTime fechaImpresionEtiqueta;

    public static Pedido recibir(Long clienteId, String direccionOrigen, String ciudadOrigen, String codigoPostalOrigen,
                                 String direccionDestino, String ciudadDestino, String codigoPostalDestino,
                                 String descripcionPaquete, Double pesoKg, Double largoCm, Double anchoCm,
                                 Double altoCm, TipoServicio tipoServicio, String numeroPedido, Prioridad prioridadSugerida,
                                 String destinatarioNombre, String destinatarioTelefono, String remitenteNombre,
                                 String remitenteEmail, String remitenteTelefono) {
        return Pedido.builder().numeroPedido(numeroPedido).clienteId(clienteId)
                .direccionOrigen(direccionOrigen).ciudadOrigen(ciudadOrigen).codigoPostalOrigen(codigoPostalOrigen)
                .direccionDestino(direccionDestino).ciudadDestino(ciudadDestino).codigoPostalDestino(codigoPostalDestino)
                .descripcionPaquete(descripcionPaquete).destinatarioNombre(destinatarioNombre)
                .destinatarioTelefono(destinatarioTelefono).remitenteNombre(remitenteNombre)
                .remitenteEmail(remitenteEmail).remitenteTelefono(remitenteTelefono)
                .pesoKg(pesoKg).largoCm(largoCm).anchoCm(anchoCm).altoCm(altoCm).tipoServicio(tipoServicio)
                .prioridadSugerida(prioridadSugerida).estado(EstadoPedido.SOLICITADO)
                .fechaCreacion(LocalDateTime.now()).build();
    }

    public void corregir(String direccionOrigen, String ciudadOrigen, String codigoPostalOrigen,
                         String direccionDestino, String ciudadDestino, String codigoPostalDestino,
                         String descripcionPaquete, Double pesoKg, Double largoCm, Double anchoCm,
                         Double altoCm, TipoServicio tipoServicio, String destinatarioNombre,
                         String destinatarioTelefono, String remitenteTelefono, Prioridad sugerida) {
        if (estado != EstadoPedido.CORRECCION_SOLICITADA) throw new IllegalStateException("El pedido no tiene correcciones pendientes");
        this.direccionOrigen = direccionOrigen; this.ciudadOrigen = ciudadOrigen;
        this.codigoPostalOrigen = codigoPostalOrigen; this.direccionDestino = direccionDestino;
        this.ciudadDestino = ciudadDestino; this.codigoPostalDestino = codigoPostalDestino;
        this.descripcionPaquete = descripcionPaquete; this.pesoKg = pesoKg; this.largoCm = largoCm;
        this.anchoCm = anchoCm; this.altoCm = altoCm; this.tipoServicio = tipoServicio;
        this.destinatarioNombre = destinatarioNombre; this.destinatarioTelefono = destinatarioTelefono;
        this.remitenteTelefono = remitenteTelefono;
        this.prioridadSugerida = sugerida; this.prioridadConfirmada = null;
        this.observacionesValidacion = null; this.justificacionPrioridad = null;
        this.operadorValidadorId = null; this.fechaValidacion = null;
        this.estado = EstadoPedido.SOLICITADO;
    }

    public void validar(boolean aprobar, Prioridad prioridadSolicitada, String observaciones,
                        String justificacion, Long operadorId) {
        if (estado != EstadoPedido.SOLICITADO || fechaValidacion != null)
            throw new TransicionEstadoInvalidaException(estado, EstadoPedido.SOLICITADO);
        Prioridad finalPrioridad = prioridadSolicitada != null ? prioridadSolicitada : prioridadSugerida;
        if (aprobar && finalPrioridad != prioridadSugerida && (justificacion == null || justificacion.isBlank())) {
            throw new IllegalArgumentException("Debe justificar el cambio de prioridad sugerida");
        }
        this.prioridadConfirmada = aprobar ? finalPrioridad : null;
        this.observacionesValidacion = observaciones;
        this.justificacionPrioridad = justificacion;
        this.operadorValidadorId = operadorId;
        this.fechaValidacion = LocalDateTime.now();
        this.estado = aprobar ? EstadoPedido.SOLICITADO : EstadoPedido.RECHAZADO;
    }

    // Compatibilidad de llamadas internas: el endpoint público exige justificación explícita.
    public void validar(boolean aprobar, Prioridad prioridadSolicitada, String observaciones, Long operadorId) {
        String justificacion = prioridadSolicitada != null && prioridadSolicitada != prioridadSugerida
                ? "Ajuste de prioridad" : null;
        validar(aprobar, prioridadSolicitada, observaciones, justificacion, operadorId);
    }

    public void solicitarCorreccion(String observaciones, Long operadorId) {
        if (estado != EstadoPedido.SOLICITADO || fechaValidacion != null)
            throw new IllegalStateException("El pedido ya no admite correcciones");
        if (observaciones == null || observaciones.isBlank()) throw new IllegalArgumentException("Debe indicar la corrección solicitada");
        this.estado = EstadoPedido.CORRECCION_SOLICITADA;
        this.observacionesValidacion = observaciones;
        this.operadorValidadorId = operadorId;
        this.fechaValidacion = LocalDateTime.now();
    }

    public void activarTracking(String tracking) {
        if (numeroTracking != null && !numeroTracking.isBlank()) return;
        if (estado == EstadoPedido.SOLICITADO && fechaValidacion == null)
            throw new IllegalStateException("El pedido debe estar validado antes de activar el tracking");
        if (!estado.puedeActivarTracking()) throw new TransicionEstadoInvalidaException(estado, EstadoPedido.CREADO);
        this.numeroTracking = tracking;
        this.fechaActivacionTracking = LocalDateTime.now();
        this.estado = EstadoPedido.CREADO;
    }

    public void cambiarEstadoLogistico(EstadoPedido nuevo) {
        boolean valido = switch (estado) {
            case CREADO -> nuevo == EstadoPedido.RECIBIDO_EN_ORIGEN || nuevo == EstadoPedido.EN_TRANSITO;
            case RECIBIDO_EN_ORIGEN -> nuevo == EstadoPedido.EN_TRANSITO || nuevo == EstadoPedido.EN_REPARTO;
            case EN_TRANSITO -> nuevo == EstadoPedido.EN_REPARTO;
            case EN_REPARTO -> nuevo == EstadoPedido.ENTREGADO;
            default -> false;
        };
        if (!valido) throw new TransicionEstadoInvalidaException(estado, nuevo);
        this.estado = nuevo;
    }

    public void confirmarImpresionEtiqueta() {
        if (numeroTracking == null) throw new TrackingNoActivoException(id);
        this.etiquetaImpresa = true;
        this.fechaImpresionEtiqueta = LocalDateTime.now();
    }
}
