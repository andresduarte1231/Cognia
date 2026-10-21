package com.cognia.pedidos.controller;

import com.cognia.pedidos.dto.Dtos.*;
import com.cognia.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService svc;

    public PedidoController(PedidoService svc) { this.svc = svc; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto crear(@Valid @RequestBody CrearPedidoRequest r, @RequestHeader(value = "Idempotency-Key", required = false) String clave) {
        return svc.crear(r, clave);
    }

    @GetMapping
    public PageDto<PedidoDto> mios(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamano) { return svc.misPedidos(pagina, tamano); }

    @GetMapping("/{id}")
    public PedidoDto ver(@PathVariable Long id) { return svc.verAutorizado(id); }

    @PostMapping("/{id}/pago")
    public PedidoDto pago(@PathVariable Long id, @Valid @RequestBody PagoRequest r) { return svc.simularPago(id, r.resultado()); }

    @PostMapping("/{id}/cancelar")
    public PedidoDto cancelar(@PathVariable Long id) { return svc.cancelarCliente(id); }
}