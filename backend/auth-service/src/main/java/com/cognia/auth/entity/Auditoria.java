package com.cognia.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

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
    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;
}