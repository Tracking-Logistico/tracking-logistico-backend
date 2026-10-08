package com.udea.demo.usuarios.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "preferencias_notificacion")
public class PreferenciaNotificacion {

    @Id
    @Column(name = "id_cliente")
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalNotificacion canal = CanalNotificacion.EMAIL;

    @Column(name = "telefono_sms", length = 20)
    private String telefonoSms;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    protected PreferenciaNotificacion() {}

    public PreferenciaNotificacion(Long clienteId) { this.clienteId = clienteId; }

    public void actualizar(CanalNotificacion canal, String telefonoSms) {
        this.canal = canal;
        this.telefonoSms = telefonoSms;
        this.activo = true;
        this.fechaActualizacion = LocalDateTime.now();
    }

    /** Atajos de negocio: encapsulan el enum y evitan que el servicio lo conozca. */
    public boolean prefiereEmail() { return canal.incluyeEmail(); }
    public boolean prefiereSms()   { return canal.incluyeSms(); }

    public Long getClienteId() { return clienteId; }
    public CanalNotificacion getCanal() { return canal; }
    public String getTelefonoSms() { return telefonoSms; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}