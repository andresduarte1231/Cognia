package com.cognia.pedidos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pagos_simulados")
@Getter
@Setter
public class PagoSimulado {
    public enum Estado { PENDIENTE, APROBADO, RECHAZADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;
    @Column(nullable = false, length = 80)
    private String referencia;
    @Column(nullable = false, length = 50)
    private String metodo = "SIMULACION";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.PENDIENTE;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private Instant actualizadoEn;
}