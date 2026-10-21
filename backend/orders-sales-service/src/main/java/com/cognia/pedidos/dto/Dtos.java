package com.cognia.pedidos.dto;

import com.cognia.pedidos.entity.Pedido;
import com.cognia.pedidos.entity.PagoSimulado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record ItemRequest(@NotNull Long productoId, @NotNull @Min(1) @Max(100) Integer cantidad) {}

    public record CrearPedidoRequest(@NotEmpty @Size(max = 50) List<@Valid ItemRequest> items, @NotNull Pedido.Entrega metodoEntrega,
                                     @Size(max = 255) String direccionEntrega, @Size(max = 120) String barrio, @Size(max = 120) String ciudad,
                                     @Size(max = 500) String observaciones) {}

    public record PagoRequest(@NotNull PagoSimulado.Estado resultado) {}

    public record EstadoPedidoRequest(@NotNull Pedido.Estado estado) {}

    public record DetalleDto(Long productoId, String nombreProducto, BigDecimal precioUnitario, Integer cantidad, BigDecimal subtotal) {}

    public record PagoDto(Long id, String referencia, String metodo, PagoSimulado.Estado estado, BigDecimal importe, Long pedidoId, Instant creadoEn) {}

    public record PedidoDto(Long id, String numeroPedido, Long clienteId, Pedido.Entrega metodoEntrega, String direccionEntrega, String barrio, String ciudad,
                            String observacionesEntrega, Pedido.Estado estado, BigDecimal subtotal, BigDecimal costoEntrega, BigDecimal total,
                            Instant creadoEn, List<DetalleDto> detalles, PagoDto pago) {}

    public record PageDto<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {}

    public record ResumenPedidos(java.util.Map<String, Long> pedidosPorEstado, BigDecimal ventasConfirmadas, long cantidadVentas,
                                 java.util.Map<String, Long> pagosPorEstado) {}

    public record VentaPeriodo(String fecha, long pedidos, BigDecimal total) {}

    public record ProductoVendido(Long productoId, String nombre, long unidades, BigDecimal ingresos) {}

    /** Respuesta de /api/productos/lote del servicio de catalogo (solo campos usados). */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductoCatalogo(Long id, String nombre, BigDecimal precio, Integer existencias, String estado, boolean disponible) {}
}