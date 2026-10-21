package com.cognia.catalogo.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
public class MovimientoInventario {
    public enum Tipo { ENTRADA, SALIDA, AJUSTE, RESERVA, LIBERACION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "producto_id", nullable = false)
    private Long productoId;
    @Column(name = "usuario_id")
    private Long usuarioId;
    @Column(name = "pedido_id")
    private Long pedidoId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(nullable = false)
    private String motivo;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;
}