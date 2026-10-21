package com.cognia.catalogo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Tabla propiedad del servicio de usuarios; este servicio solo inserta registros de sus propias acciones. */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usuario_id")
    private Long usuarioId;
    @Column(nullable = false, length = 100)
    private String accion;
    @Column(nullable = false, length = 100)
    private String recurso;
    @Column(name = "recurso_id", length = 100)
    private String recursoId;
    @Column(nullable = false)
    private String resultado = "EXITOSO";
    @Column(columnDefinition = "text")
    private String detalle;
    @Column(name = "correlacion_id", length = 100)
    private String correlacionId;
}