package com.cognia.pedidos.controller;

import com.cognia.pedidos.dto.Dtos.*;
import com.cognia.pedidos.entity.PagoSimulado;
import com.cognia.pedidos.entity.Pedido;
import com.cognia.pedidos.service.PedidoService;
import com.cognia.pedidos.service.ReporteService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminPedidoController {
    private final PedidoService pedidos;
    private final ReporteService reportes;

    public AdminPedidoController(PedidoService pedidos, ReporteService reportes) { this.pedidos = pedidos; this.reportes = reportes; }

    @GetMapping("/pedidos")
    public PageDto<PedidoDto> buscar(@RequestParam(required = false) Pedido.Estado estado, @RequestParam(required = false) String numero,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return pedidos.buscar(estado, numero, desde, hasta, pagina, tamano);
    }

    @GetMapping("/pedidos/{id}")
    public PedidoDto ver(@PathVariable Long id) { return pedidos.verAutorizado(id); }

    @PatchMapping("/pedidos/{id}/estado")
    public PedidoDto estado(@PathVariable Long id, @Valid @RequestBody EstadoPedidoRequest r) { return pedidos.cambiarEstado(id, r.estado()); }

    @GetMapping("/pagos")
    public PageDto<PagoDto> pagos(@RequestParam(required = false) PagoSimulado.Estado estado,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                  @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return reportes.pagos(estado, desde, hasta, pagina, tamano);
    }

    @GetMapping("/reportes/resumen")
    public ResumenPedidos resumen() { return reportes.resumen(); }

    @GetMapping("/reportes/ventas")
    public List<VentaPeriodo> ventas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.ventas(desde, hasta);
    }

    @GetMapping("/reportes/mas-vendidos")
    public List<ProductoVendido> masVendidos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @RequestParam(defaultValue = "10") int limite) {
        return reportes.masVendidos(desde, hasta, limite);
    }

    @GetMapping("/reportes/ventas.csv")
    public ResponseEntity<String> ventasCsv(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return csv("ventas.csv", reportes.ventasCsv(desde, hasta));
    }

    @GetMapping("/reportes/mas-vendidos.csv")
    public ResponseEntity<String> masVendidosCsv(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return csv("mas-vendidos.csv", reportes.masVendidosCsv(desde, hasta));
    }

    private ResponseEntity<String> csv(String nombre, String cuerpo) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombre)
            .contentType(MediaType.parseMediaType("text/csv; charset=utf-8")).body(cuerpo);
    }
}