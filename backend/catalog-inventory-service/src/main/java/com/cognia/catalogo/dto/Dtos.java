package com.cognia.catalogo.dto;

import com.cognia.catalogo.entity.MovimientoInventario;
import com.cognia.catalogo.entity.Producto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public static final String IMG_PATTERN = "^(/uploads/[A-Za-z0-9._-]+|/placeholder\\.svg|https://[^\\s\"'<>]+)$";

    public record ProductoDto(Long id, Long categoriaId, String categoriaNombre, String nombre, String descripcion,
                              BigDecimal precio, BigDecimal precioAnterior, boolean enPromocion, Integer existencias,
                              Integer inventarioMinimo, String imagenUrl, Producto.Estado estado, boolean disponible) {}

    public record ProductoRequest(@NotNull Long categoriaId, @NotBlank @Size(max = 160) String nombre,
                                  @Size(max = 2000) String descripcion,
                                  @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precio,
                                  @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precioAnterior,
                                  @Min(0) @Max(1000000) Integer existencias, @Min(0) @Max(1000000) Integer inventarioMinimo,
                                  @Pattern(regexp = IMG_PATTERN) @Size(max = 500) String imagenUrl,
                                  Producto.Estado estado) {}

    public record PrecioRequest(@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precio,
                                @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precioAnterior) {}

    public record EstadoProductoRequest(@NotNull Producto.Estado estado) {}

    public record CategoriaDto(Long id, String nombre, String descripcion, String estado) {}

    public record CategoriaRequest(@NotBlank @Size(max = 100) String nombre, @Size(max = 1000) String descripcion,
                                   @Pattern(regexp = "ACTIVA|INACTIVA") String estado) {}

    public record MovimientoRequest(@NotNull Long productoId, @NotNull MovimientoInventario.Tipo tipo,
                                    @NotNull @Min(0) @Max(1000000) Integer cantidad, @NotBlank @Size(max = 200) String motivo) {}

    public record MovimientoDto(Long id, Long productoId, Long usuarioId, Long pedidoId, MovimientoInventario.Tipo tipo,
                                Integer cantidad, String motivo, Instant creadoEn) {}

    public record ItemReserva(@NotNull Long productoId, @NotNull @Min(1) @Max(1000) Integer cantidad) {}

    public record ReservaRequest(@NotNull Long pedidoId, @NotEmpty @Size(max = 100) List<@Valid ItemReserva> items) {}

    public record ReferenciaPedidoRequest(@NotNull Long pedidoId) {}

    public record ResumenInventario(long productosActivos, long productosInactivos, long productosAgotados,
                                    long existenciasBajas, long categorias) {}

    public record ConfiguracionRequest(@NotEmpty java.util.Map<@Size(max = 100) String, @Size(max = 2000) String> valores) {}

    public record PageDto<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {}
}