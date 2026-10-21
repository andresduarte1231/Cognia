package com.cognia.catalogo.service;

import com.cognia.catalogo.dto.Dtos.*;
import com.cognia.catalogo.entity.MovimientoInventario;
import com.cognia.catalogo.entity.MovimientoInventario.Tipo;
import com.cognia.catalogo.entity.Producto;
import com.cognia.catalogo.exception.BusinessException;
import com.cognia.catalogo.exception.ConflictException;
import com.cognia.catalogo.exception.NotFoundException;
import com.cognia.catalogo.mapper.CatalogoMapper;
import com.cognia.catalogo.repository.*;
import com.cognia.catalogo.security.Actor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventario: existencias = unidades disponibles. RESERVA descuenta existencias; LIBERACION las devuelve;
 * SALIDA registrada al confirmar un pedido es solo un asiento (no vuelve a descontar).
 * Todas las operaciones bloquean filas (SELECT ... FOR UPDATE, orden por id) para evitar inventario negativo.
 */
@Service
public class InventarioService {
    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;
    private final CategoriaRepository categorias;
    private final CatalogoMapper mapper;
    private final AuditoriaService auditoria;
    private final Counter reservasOk;
    private final Counter reservasSinStock;

    public InventarioService(ProductoRepository productos, MovimientoRepository movimientos, CategoriaRepository categorias,
                             CatalogoMapper mapper, AuditoriaService auditoria, MeterRegistry registry) {
        this.productos = productos; this.movimientos = movimientos; this.categorias = categorias; this.mapper = mapper; this.auditoria = auditoria;
        this.reservasOk = Counter.builder("cognia_inventario_reservas_total").tag("resultado", "ok").register(registry);
        this.reservasSinStock = Counter.builder("cognia_inventario_reservas_total").tag("resultado", "sin_stock").register(registry);
    }

    @Transactional
    public MovimientoDto movimientoManual(MovimientoRequest r) {
        if (r.tipo() == Tipo.RESERVA || r.tipo() == Tipo.LIBERACION) {
            throw new BusinessException("Las reservas y liberaciones solo se generan desde pedidos");
        }
        Producto p = productos.bloquearPorIds(List.of(r.productoId())).stream().findFirst().orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        int actual = p.getExistencias();
        int nuevo;
        int cantidadMov = r.cantidad();
        String motivo = r.motivo().trim();
        switch (r.tipo()) {
            case ENTRADA -> {
                if (r.cantidad() < 1) throw new BusinessException("La cantidad debe ser mayor a cero");
                nuevo = actual + r.cantidad();
            }
            case SALIDA -> {
                if (r.cantidad() < 1) throw new BusinessException("La cantidad debe ser mayor a cero");
                if (r.cantidad() > actual) throw new ConflictException("Existencias insuficientes");
                nuevo = actual - r.cantidad();
            }
            default -> {
                nuevo = r.cantidad();
                if (nuevo == actual) throw new BusinessException("El ajuste no modifica las existencias");
                cantidadMov = Math.abs(nuevo - actual);
                motivo = "AJUSTE " + actual + "->" + nuevo + ": " + motivo;
            }
        }
        p.setExistencias(nuevo);
        ProductoService.normalizar(p);
        MovimientoInventario m = guardar(p.getId(), null, r.tipo(), cantidadMov, motivo);
        auditoria.registrar("MOVIMIENTO_INVENTARIO", "productos", p.getId(), r.tipo() + " " + cantidadMov);
        return mapper.toDto(m);
    }

    @Transactional
    public void reservar(ReservaRequest r) {
        if (movimientos.existsByPedidoIdAndTipo(r.pedidoId(), Tipo.RESERVA)) return; // idempotente
        Map<Long, Integer> pedido = new TreeMap<>();
        for (ItemReserva i : r.items()) pedido.merge(i.productoId(), i.cantidad(), Integer::sum);
        Map<Long, Producto> bloqueados = productos.bloquearPorIds(pedido.keySet()).stream().collect(Collectors.toMap(Producto::getId, p -> p));
        List<String> problemas = new ArrayList<>();
        pedido.forEach((id, cant) -> {
            Producto p = bloqueados.get(id);
            if (p == null || p.getEstado() == Producto.Estado.INACTIVO) problemas.add("Producto " + id + " no disponible");
            else if (p.getExistencias() < cant) problemas.add(p.getNombre() + ": existencias insuficientes");
        });
        if (!problemas.isEmpty()) {
            reservasSinStock.increment();
            throw new ConflictException(String.join("; ", problemas));
        }
        pedido.forEach((id, cant) -> {
            Producto p = bloqueados.get(id);
            p.setExistencias(p.getExistencias() - cant);
            ProductoService.normalizar(p);
            guardar(id, r.pedidoId(), Tipo.RESERVA, cant, "Reserva de pedido");
        });
        reservasOk.increment();
    }

    @Transactional
    public void liberar(Long pedidoId) {
        if (movimientos.existsByPedidoIdAndTipo(pedidoId, Tipo.LIBERACION)) return;
        List<MovimientoInventario> reservas = movimientos.findByPedidoIdAndTipo(pedidoId, Tipo.RESERVA);
        if (reservas.isEmpty()) return;
        Map<Long, Producto> bloqueados = productos.bloquearPorIds(reservas.stream().map(MovimientoInventario::getProductoId).collect(Collectors.toSet()))
            .stream().collect(Collectors.toMap(Producto::getId, p -> p));
        for (MovimientoInventario res : reservas) {
            Producto p = bloqueados.get(res.getProductoId());
            if (p == null) continue;
            p.setExistencias(p.getExistencias() + res.getCantidad());
            ProductoService.normalizar(p);
            guardar(p.getId(), pedidoId, Tipo.LIBERACION, res.getCantidad(), "Liberacion de reserva de pedido");
        }
    }

    @Transactional
    public void confirmar(Long pedidoId) {
        if (movimientos.existsByPedidoIdAndTipo(pedidoId, Tipo.SALIDA) || movimientos.existsByPedidoIdAndTipo(pedidoId, Tipo.LIBERACION)) return;
        for (MovimientoInventario res : movimientos.findByPedidoIdAndTipo(pedidoId, Tipo.RESERVA)) {
            guardar(res.getProductoId(), pedidoId, Tipo.SALIDA, res.getCantidad(), "Venta confirmada (existencias ya descontadas en la reserva)");
        }
    }

    @Transactional(readOnly = true)
    public PageDto<MovimientoDto> listar(Long productoId, int page, int size) {
        return mapper.page(movimientos.listar(productoId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.unsorted())), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public ResumenInventario resumen() {
        return new ResumenInventario(productos.countByEstado(Producto.Estado.ACTIVO), productos.countByEstado(Producto.Estado.INACTIVO),
            productos.countByEstado(Producto.Estado.AGOTADO), productos.contarBajos(), categorias.count());
    }

    @Transactional(readOnly = true)
    public List<ProductoDto> alertas() {
        return productos.alertasBajas().stream().map(mapper::toDto).toList();
    }

    private MovimientoInventario guardar(Long productoId, Long pedidoId, Tipo tipo, int cantidad, String motivo) {
        MovimientoInventario m = new MovimientoInventario();
        m.setProductoId(productoId); m.setPedidoId(pedidoId); m.setUsuarioId(Actor.id()); m.setTipo(tipo);
        m.setCantidad(cantidad); m.setMotivo(motivo.length() > 250 ? motivo.substring(0, 250) : motivo);
        return movimientos.save(m);
    }
}