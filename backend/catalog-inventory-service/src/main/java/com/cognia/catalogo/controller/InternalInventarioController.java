package com.cognia.catalogo.controller;

import com.cognia.catalogo.dto.Dtos.*;
import com.cognia.catalogo.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Solo accesible con token de servicio (rol SERVICIO). El API Gateway no enruta /internal/**. */
@RestController
@RequestMapping("/internal/inventario")
public class InternalInventarioController {
    private final InventarioService inventario;

    public InternalInventarioController(InventarioService inventario) { this.inventario = inventario; }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reservar(@Valid @RequestBody ReservaRequest r) { inventario.reservar(r); }

    @PostMapping("/liberaciones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liberar(@Valid @RequestBody ReferenciaPedidoRequest r) { inventario.liberar(r.pedidoId()); }

    @PostMapping("/confirmaciones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmar(@Valid @RequestBody ReferenciaPedidoRequest r) { inventario.confirmar(r.pedidoId()); }
}