package com.cognia.catalogo.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categorias")
@Getter
@Setter
public class Categoria {
    public enum Estado { ACTIVA, INACTIVA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(columnDefinition = "text")
    private String descripcion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.ACTIVA;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;
}