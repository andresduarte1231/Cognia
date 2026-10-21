package com.cognia.pedidos.service;

import com.cognia.pedidos.dto.Dtos.*;
import com.cognia.pedidos.entity.PagoSimulado;
import com.cognia.pedidos.mapper.PedidoMapper;
import com.cognia.pedidos.repository.PagoRepository;
import com.cognia.pedidos.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Una venta confirmada es un pedido con pago APROBADO que no fue cancelado. Los pagos rechazados/pendientes nunca suman. */
@Service
@Transactional(readOnly = true)
public class ReporteService {
    private static final ZoneId ZONA = ZoneId.systemDefault();
    private final PedidoRepository pedidos;
    private final PagoRepository pagos;
    private final PedidoMapper mapper;

    public ReporteService(PedidoRepository pedidos, PagoRepository pagos, PedidoMapper mapper) { this.pedidos = pedidos; this.pagos = pagos; this.mapper = mapper; }

    public ResumenPedidos resumen() {
        Map<String, Long> porEstado = new LinkedHashMap<>();
        pedidos.contarPorEstado().forEach(e -> porEstado.put(e.getEstado().name(), e.getTotal()));
        Instant d = LocalDate.of(2000, 1, 1).atStartOfDay(ZONA).toInstant(), h = LocalDate.of(2100, 1, 1).atStartOfDay(ZONA).toInstant();
        var ventas = pedidos.ventasPorDia(d, h);
        BigDecimal total = ventas.stream().map(v -> v.getTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
        long n = ventas.stream().mapToLong(v -> v.getPedidos()).sum();
        Map<String, Long> pagosEstado = new LinkedHashMap<>();
        pagos.resumen().forEach(p -> pagosEstado.put(p.getEstado().name(), p.getTotal()));
        return new ResumenPedidos(porEstado, total, n, pagosEstado);
    }

    public List<VentaPeriodo> ventas(LocalDate desde, LocalDate hasta) {
        return pedidos.ventasPorDia(inicio(desde), fin(hasta)).stream().map(v -> new VentaPeriodo(v.getDia().toString(), v.getPedidos(), v.getTotal())).toList();
    }

    public List<ProductoVendido> masVendidos(LocalDate desde, LocalDate hasta, int limite) {
        return pedidos.masVendidos(inicio(desde), fin(hasta), Math.min(Math.max(limite, 1), 50)).stream()
            .map(m -> new ProductoVendido(m.getProductoId(), m.getNombre(), m.getUnidades(), m.getIngresos())).toList();
    }

    public PageDto<PagoDto> pagos(PagoSimulado.Estado estado, LocalDate desde, LocalDate hasta, int page, int size) {
        return mapper.page(pagos.buscar(estado, inicio(desde), fin(hasta), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))), mapper::toDto);
    }

    public String ventasCsv(LocalDate desde, LocalDate hasta) {
        StringBuilder sb = new StringBuilder("fecha,pedidos,total\n");
        ventas(desde, hasta).forEach(v -> sb.append(csv(v.fecha())).append(',').append(v.pedidos()).append(',').append(v.total().toPlainString()).append('\n'));
        return sb.toString();
    }

    public String masVendidosCsv(LocalDate desde, LocalDate hasta) {
        StringBuilder sb = new StringBuilder("producto,unidades,ingresos\n");
        masVendidos(desde, hasta, 50).forEach(m -> sb.append(csv(m.nombre())).append(',').append(m.unidades()).append(',').append(m.ingresos().toPlainString()).append('\n'));
        return sb.toString();
    }

    /** Neutraliza inyeccion de formulas en hojas de calculo y escapa comillas. */
    static String csv(String v) {
        if (v == null) return "";
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private Instant inicio(LocalDate d) { return (d == null ? LocalDate.of(2000, 1, 1) : d).atStartOfDay(ZONA).toInstant(); }
    private Instant fin(LocalDate d) { return (d == null ? LocalDate.of(2100, 1, 1) : d.plusDays(1)).atStartOfDay(ZONA).toInstant(); }
}