package com.cognia.catalogo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "productos")
@Getter
@Setter
public class Producto {
    public enum Estado { ACTIVO, INACTIVO, AGOTADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
    @Column(nullable = false, length = 160)
    private String nombre;
    @Column(columnDefinition = "text")
    private String descripcion;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;
    @Column(name = "precio_anterior", precision = 12, scale = 2)
    private BigDecimal precioAnterior;
    @Column(nullable = false)
    private Integer existencias = 0;
    @Column(name = "inventario_minimo", nullable = false)
    private Integer inventarioMinimo = 5;
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.ACTIVO;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private Instant actualizadoEn;
}