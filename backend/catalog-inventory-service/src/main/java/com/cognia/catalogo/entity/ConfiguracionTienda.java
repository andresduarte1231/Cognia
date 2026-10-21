package com.cognia.catalogo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "configuracion_tienda")
@Getter
@Setter
public class ConfiguracionTienda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String clave;
    @Column(columnDefinition = "text")
    private String valor;
}