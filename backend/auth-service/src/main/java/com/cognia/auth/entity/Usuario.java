package com.cognia.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {
    public enum Rol { PROPIETARIO, ADMINISTRADOR, EMPLEADO_VENTAS, CLIENTE }
    public enum Estado { ACTIVO, INACTIVO, BLOQUEADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(nullable = false, length = 190)
    private String correo;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol = Rol.CLIENTE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.ACTIVO;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private Instant actualizadoEn;
}