package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.TrackingNoActivoException;
import com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
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
    @Version @Column(name = "version", nullable = false) private Long version = 0L;
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
    @Column(name = "etiqueta_impresa", nullable = false) private Boolean etiquetaImpresa = false;
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

    public Pedido() {}
    public Pedido(Long id, String numeroPedido, Long clienteId, String direccionOrigen, String direccionDestino, String descripcionPaquete, String ciudadOrigen, String codigoPostalOrigen, String ciudadDestino, String codigoPostalDestino, String remitenteNombre, String remitenteEmail, String remitenteTelefono, Long version, String destinatarioNombre, String destinatarioTelefono, Double pesoKg, Double largoCm, Double anchoCm, Double altoCm, TipoServicio tipoServicio, Prioridad prioridadSugerida, Prioridad prioridadConfirmada, EstadoPedido estado, String observacionesValidacion, String justificacionPrioridad, Long operadorValidadorId, LocalDateTime fechaCreacion, LocalDateTime fechaValidacion, String numeroTracking, LocalDateTime fechaActivacionTracking, Boolean etiquetaImpresa, LocalDateTime fechaImpresionEtiqueta) {
        this.id = id;
        this.numeroPedido = numeroPedido;
        this.clienteId = clienteId;
        this.direccionOrigen = direccionOrigen;
        this.direccionDestino = direccionDestino;
        this.descripcionPaquete = descripcionPaquete;
        this.ciudadOrigen = ciudadOrigen;
        this.codigoPostalOrigen = codigoPostalOrigen;
        this.ciudadDestino = ciudadDestino;
        this.codigoPostalDestino = codigoPostalDestino;
        this.remitenteNombre = remitenteNombre;
        this.remitenteEmail = remitenteEmail;
        this.remitenteTelefono = remitenteTelefono;
        this.version = version;
        this.destinatarioNombre = destinatarioNombre;
        this.destinatarioTelefono = destinatarioTelefono;
        this.pesoKg = pesoKg;
        this.largoCm = largoCm;
        this.anchoCm = anchoCm;
        this.altoCm = altoCm;
        this.tipoServicio = tipoServicio;
        this.prioridadSugerida = prioridadSugerida;
        this.prioridadConfirmada = prioridadConfirmada;
        this.estado = estado;
        this.observacionesValidacion = observacionesValidacion;
        this.justificacionPrioridad = justificacionPrioridad;
        this.operadorValidadorId = operadorValidadorId;
        this.fechaCreacion = fechaCreacion;
        this.fechaValidacion = fechaValidacion;
        this.numeroTracking = numeroTracking;
        this.fechaActivacionTracking = fechaActivacionTracking;
        this.etiquetaImpresa = etiquetaImpresa;
        this.fechaImpresionEtiqueta = fechaImpresionEtiqueta;
    }

    public Long getId() { return id; }
    public String getNumeroPedido() { return numeroPedido; }
    public Long getClienteId() { return clienteId; }
    public String getDireccionOrigen() { return direccionOrigen; }
    public String getDireccionDestino() { return direccionDestino; }
    public String getDescripcionPaquete() { return descripcionPaquete; }
    public String getCiudadOrigen() { return ciudadOrigen; }
    public String getCodigoPostalOrigen() { return codigoPostalOrigen; }
    public String getCiudadDestino() { return ciudadDestino; }
    public String getCodigoPostalDestino() { return codigoPostalDestino; }
    public String getRemitenteNombre() { return remitenteNombre; }
    public String getRemitenteEmail() { return remitenteEmail; }
    public String getRemitenteTelefono() { return remitenteTelefono; }
    public Long getVersion() { return version; }
    public String getDestinatarioNombre() { return destinatarioNombre; }
    public String getDestinatarioTelefono() { return destinatarioTelefono; }
    public Double getPesoKg() { return pesoKg; }
    public Double getLargoCm() { return largoCm; }
    public Double getAnchoCm() { return anchoCm; }
    public Double getAltoCm() { return altoCm; }
    public TipoServicio getTipoServicio() { return tipoServicio; }
    public Prioridad getPrioridadSugerida() { return prioridadSugerida; }
    public Prioridad getPrioridadConfirmada() { return prioridadConfirmada; }
    public EstadoPedido getEstado() { return estado; }
    public String getObservacionesValidacion() { return observacionesValidacion; }
    public String getJustificacionPrioridad() { return justificacionPrioridad; }
    public Long getOperadorValidadorId() { return operadorValidadorId; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaValidacion() { return fechaValidacion; }
    public String getNumeroTracking() { return numeroTracking; }
    public LocalDateTime getFechaActivacionTracking() { return fechaActivacionTracking; }
    public Boolean getEtiquetaImpresa() { return etiquetaImpresa; }
    public LocalDateTime getFechaImpresionEtiqueta() { return fechaImpresionEtiqueta; }

    public static PedidoBuilder builder() { return new PedidoBuilder(); }
    public static class PedidoBuilder {
        private Long id;
        private String numeroPedido;
        private Long clienteId;
        private String direccionOrigen;
        private String direccionDestino;
        private String descripcionPaquete;
        private String ciudadOrigen;
        private String codigoPostalOrigen;
        private String ciudadDestino;
        private String codigoPostalDestino;
        private String remitenteNombre;
        private String remitenteEmail;
        private String remitenteTelefono;
        private Long version;
        private boolean versionSet;
        private String destinatarioNombre;
        private String destinatarioTelefono;
        private Double pesoKg;
        private Double largoCm;
        private Double anchoCm;
        private Double altoCm;
        private TipoServicio tipoServicio;
        private Prioridad prioridadSugerida;
        private Prioridad prioridadConfirmada;
        private EstadoPedido estado;
        private String observacionesValidacion;
        private String justificacionPrioridad;
        private Long operadorValidadorId;
        private LocalDateTime fechaCreacion;
        private LocalDateTime fechaValidacion;
        private String numeroTracking;
        private LocalDateTime fechaActivacionTracking;
        private Boolean etiquetaImpresa;
        private boolean etiquetaImpresaSet;
        private LocalDateTime fechaImpresionEtiqueta;

        public PedidoBuilder id(Long id) { this.id = id; return this; }
        public PedidoBuilder numeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; return this; }
        public PedidoBuilder clienteId(Long clienteId) { this.clienteId = clienteId; return this; }
        public PedidoBuilder direccionOrigen(String direccionOrigen) { this.direccionOrigen = direccionOrigen; return this; }
        public PedidoBuilder direccionDestino(String direccionDestino) { this.direccionDestino = direccionDestino; return this; }
        public PedidoBuilder descripcionPaquete(String descripcionPaquete) { this.descripcionPaquete = descripcionPaquete; return this; }
        public PedidoBuilder ciudadOrigen(String ciudadOrigen) { this.ciudadOrigen = ciudadOrigen; return this; }
        public PedidoBuilder codigoPostalOrigen(String codigoPostalOrigen) { this.codigoPostalOrigen = codigoPostalOrigen; return this; }
        public PedidoBuilder ciudadDestino(String ciudadDestino) { this.ciudadDestino = ciudadDestino; return this; }
        public PedidoBuilder codigoPostalDestino(String codigoPostalDestino) { this.codigoPostalDestino = codigoPostalDestino; return this; }
        public PedidoBuilder remitenteNombre(String remitenteNombre) { this.remitenteNombre = remitenteNombre; return this; }
        public PedidoBuilder remitenteEmail(String remitenteEmail) { this.remitenteEmail = remitenteEmail; return this; }
        public PedidoBuilder remitenteTelefono(String remitenteTelefono) { this.remitenteTelefono = remitenteTelefono; return this; }
        public PedidoBuilder version(Long version) { this.version = version; this.versionSet = true; return this; }
        public PedidoBuilder destinatarioNombre(String destinatarioNombre) { this.destinatarioNombre = destinatarioNombre; return this; }
        public PedidoBuilder destinatarioTelefono(String destinatarioTelefono) { this.destinatarioTelefono = destinatarioTelefono; return this; }
        public PedidoBuilder pesoKg(Double pesoKg) { this.pesoKg = pesoKg; return this; }
        public PedidoBuilder largoCm(Double largoCm) { this.largoCm = largoCm; return this; }
        public PedidoBuilder anchoCm(Double anchoCm) { this.anchoCm = anchoCm; return this; }
        public PedidoBuilder altoCm(Double altoCm) { this.altoCm = altoCm; return this; }
        public PedidoBuilder tipoServicio(TipoServicio tipoServicio) { this.tipoServicio = tipoServicio; return this; }
        public PedidoBuilder prioridadSugerida(Prioridad prioridadSugerida) { this.prioridadSugerida = prioridadSugerida; return this; }
        public PedidoBuilder prioridadConfirmada(Prioridad prioridadConfirmada) { this.prioridadConfirmada = prioridadConfirmada; return this; }
        public PedidoBuilder estado(EstadoPedido estado) { this.estado = estado; return this; }
        public PedidoBuilder observacionesValidacion(String observacionesValidacion) { this.observacionesValidacion = observacionesValidacion; return this; }
        public PedidoBuilder justificacionPrioridad(String justificacionPrioridad) { this.justificacionPrioridad = justificacionPrioridad; return this; }
        public PedidoBuilder operadorValidadorId(Long operadorValidadorId) { this.operadorValidadorId = operadorValidadorId; return this; }
        public PedidoBuilder fechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; return this; }
        public PedidoBuilder fechaValidacion(LocalDateTime fechaValidacion) { this.fechaValidacion = fechaValidacion; return this; }
        public PedidoBuilder numeroTracking(String numeroTracking) { this.numeroTracking = numeroTracking; return this; }
        public PedidoBuilder fechaActivacionTracking(LocalDateTime fechaActivacionTracking) { this.fechaActivacionTracking = fechaActivacionTracking; return this; }
        public PedidoBuilder etiquetaImpresa(Boolean etiquetaImpresa) { this.etiquetaImpresa = etiquetaImpresa; this.etiquetaImpresaSet = true; return this; }
        public PedidoBuilder fechaImpresionEtiqueta(LocalDateTime fechaImpresionEtiqueta) { this.fechaImpresionEtiqueta = fechaImpresionEtiqueta; return this; }
        public Pedido build() {
            Long versionValue = versionSet ? version : 0L;
            Boolean etiquetaImpresaValue = etiquetaImpresaSet ? etiquetaImpresa : false;
            return new Pedido(id, numeroPedido, clienteId, direccionOrigen, direccionDestino, descripcionPaquete, ciudadOrigen, codigoPostalOrigen, ciudadDestino, codigoPostalDestino, remitenteNombre, remitenteEmail, remitenteTelefono, versionValue, destinatarioNombre, destinatarioTelefono, pesoKg, largoCm, anchoCm, altoCm, tipoServicio, prioridadSugerida, prioridadConfirmada, estado, observacionesValidacion, justificacionPrioridad, operadorValidadorId, fechaCreacion, fechaValidacion, numeroTracking, fechaActivacionTracking, etiquetaImpresaValue, fechaImpresionEtiqueta);
        }
    }
}
