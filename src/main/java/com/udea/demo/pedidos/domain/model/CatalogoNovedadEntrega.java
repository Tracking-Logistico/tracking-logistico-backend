package com.udea.demo.pedidos.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "catalogo_novedades_entrega")
public class CatalogoNovedadEntrega {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50) private String codigo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 35) private ResultadoEntrega resultado;
    @Column(nullable = false, length = 255) private String descripcion;
    @Column(nullable = false) private boolean activo = true;

    protected CatalogoNovedadEntrega() {}
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public ResultadoEntrega getResultado() { return resultado; }
    public String getDescripcion() { return descripcion; }
}
