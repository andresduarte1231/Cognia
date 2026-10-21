package com.cognia.pedidos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
public class Pedido {
    public enum Estado { PENDIENTE, CONFIRMADO, EN_PREPARACION, LISTO_PARA_RECOGIDA, EN_CAMINO, ENTREGADO, CANCELADO }
    public enum Entrega { DOMICILIO, RECOGIDA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "numero_pedido", nullable = false, length = 40)
    private String numeroPedido;
    @Column(name = "clave_idempotencia", length = 80)
    private String claveIdempotencia;
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_entrega", nullable = false)
    private Entrega metodoEntrega;
    @Column(name = "direccion_entrega")
    private String direccionEntrega;
    @Column(length = 120)
    private String barrio;
    @Column(length = 120)
    private String ciudad;
    @Column(name = "observaciones_entrega", columnDefinition = "text")
    private String observacionesEntrega;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.PENDIENTE;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    @Column(name = "costo_entrega", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEntrega = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private Instant actualizadoEn;
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();
}