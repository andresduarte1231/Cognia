package com.cognia.pedidos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Conserva nombre y precio historicos del producto al momento de la compra. */
@Entity
@Table(name = "detalle_pedidos")
@Getter
@Setter
public class DetallePedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;
    @Column(name = "producto_id")
    private Long productoId;
    @Column(name = "nombre_producto", nullable = false, length = 160)
    private String nombreProducto;
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}