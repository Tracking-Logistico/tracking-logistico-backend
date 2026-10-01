package com.udea.demo.usuarios.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    private String ciudad;

    public Cliente() {}
    public Cliente(Long id, Usuario usuario, String ciudad) {
        this.id = id;
        this.usuario = usuario;
        this.ciudad = ciudad;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public static ClienteBuilder builder() { return new ClienteBuilder(); }
    public static class ClienteBuilder {
        private Long id;
        private Usuario usuario;
        private String ciudad;

        public ClienteBuilder id(Long id) { this.id = id; return this; }
        public ClienteBuilder usuario(Usuario usuario) { this.usuario = usuario; return this; }
        public ClienteBuilder ciudad(String ciudad) { this.ciudad = ciudad; return this; }
        public Cliente build() {
            return new Cliente(id, usuario, ciudad);
        }
    }
}
