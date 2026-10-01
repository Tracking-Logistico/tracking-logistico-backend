package com.udea.demo.usuarios.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "operadores")
public class Operador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_operador")
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "codigo_empleado", unique = true)
    private String codigoEmpleado;

    public Operador() {}
    public Operador(Long id, Usuario usuario, String codigoEmpleado) {
        this.id = id;
        this.usuario = usuario;
        this.codigoEmpleado = codigoEmpleado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getCodigoEmpleado() { return codigoEmpleado; }
    public void setCodigoEmpleado(String codigoEmpleado) { this.codigoEmpleado = codigoEmpleado; }

    public static OperadorBuilder builder() { return new OperadorBuilder(); }
    public static class OperadorBuilder {
        private Long id;
        private Usuario usuario;
        private String codigoEmpleado;

        public OperadorBuilder id(Long id) { this.id = id; return this; }
        public OperadorBuilder usuario(Usuario usuario) { this.usuario = usuario; return this; }
        public OperadorBuilder codigoEmpleado(String codigoEmpleado) { this.codigoEmpleado = codigoEmpleado; return this; }
        public Operador build() {
            return new Operador(id, usuario, codigoEmpleado);
        }
    }
}
