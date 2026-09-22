package com.udea.demo.usuarios.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "operadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}