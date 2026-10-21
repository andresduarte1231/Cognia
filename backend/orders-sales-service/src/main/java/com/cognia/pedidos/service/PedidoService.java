package com.cognia.pedidos.service;

import com.cognia.pedidos.dto.Dtos.*;
import com.cognia.pedidos.entity.*;
import com.cognia.pedidos.entity.Pedido.Estado;
import com.cognia.pedidos.exception.*;
import com.cognia.pedidos.mapper.PedidoMapper;
import com.cognia.pedidos.repository.*;
import com.cognia.pedidos.security.Actor;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Saga de compra (sin transaccion distribuida):
 *  1) validar y fijar precios con datos del catalogo; 2) persistir pedido PENDIENTE + pago PENDIENTE (tx local);
 *  3) reservar inventario en catalogo (idempotente por pedidoId); si falla -> compensar cancelando el pedido;
 *  4) pago simulado: APROBADO confirma el pedido; RECHAZADO libera la reserva y cancela.
 *  Un job libera reservas de pedidos PENDIENTE abandonados.
 */
@Service
public class PedidoService {
    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);
    static final Map<Estado, Set<Estado>> TRANSICIONES = Map.of(
        Estado.CONFIRMADO, Set.of(Estado.EN_PREPARACION, Estado.CANCELADO),
        Estado.EN_PREPARACION, Set.of(Estado.LISTO_PARA_RECOGIDA, Estado.EN_CAMINO, Estado.CANCELADO),
        Estado.LISTO_PARA_RECOGIDA, Set.of(Estado.ENTREGADO),
        Estado.EN_CAMINO, Set.of(Estado.ENTREGADO));
    private static final ZoneId ZONA = ZoneId.systemDefault();
    private final PedidoRepository pedidos;
    private final PagoRepository pagos;
    private final CatalogClient catalogo;
    private final PedidoMapper mapper;
    private final AuditoriaService auditoria;
    private final TransactionTemplate tx;
    private final MeterRegistry registry;
    private final SecureRandom random = new SecureRandom();

    public PedidoService(PedidoRepository pedidos, PagoRepository pagos, CatalogClient catalogo, PedidoMapper mapper, AuditoriaService auditoria,
                         PlatformTransactionManager tm, MeterRegistry registry) {
        this.pedidos = pedidos; this.pagos = pagos; this.catalogo = catalogo; this.mapper = mapper; this.auditoria = auditoria;
        this.tx = new TransactionTemplate(tm); this.registry = registry;
    }

    public PedidoDto crear(CrearPedidoRequest r, String claveIdem) {
        Long clienteId = Actor.id();
        if (claveIdem == null || !claveIdem.matches("[A-Za-z0-9-]{8,80}")) throw new BusinessException("Cabecera Idempotency-Key obligatoria (8-80 caracteres alfanumericos)");
        Optional<Pedido> previo = pedidos.findByClienteIdAndClaveIdempotencia(clienteId, claveIdem);
        if (previo.isPresent()) return ver(previo.get().getId());

        if (r.metodoEntrega() == Pedido.Entrega.DOMICILIO
            && (blank(r.direccionEntrega()) || blank(r.ciudad()))) throw new BusinessException("Domicilio requiere direccion y ciudad");

        Map<Long, Integer> cantidades = new TreeMap<>();
        for (ItemRequest i : r.items()) cantidades.merge(i.productoId(), i.cantidad(), Integer::sum);
        Map<Long, ProductoCatalogo> cat = catalogo.productos(new ArrayList<>(cantidades.keySet()));
        List<String> problemas = new ArrayList<>();
        cantidades.forEach((id, cant) -> {
            ProductoCatalogo p = cat.get(id);
            if (p == null || "INACTIVO".equals(p.estado())) problemas.add("Producto " + id + " no disponible");
            else if (!p.disponible() || p.existencias() < cant) problemas.add(p.nombre() + ": existencias insuficientes");
        });
        if (!problemas.isEmpty()) throw new ConflictException(String.join("; ", problemas));

        BigDecimal costo = BigDecimal.ZERO;
        if (r.metodoEntrega() == Pedido.Entrega.DOMICILIO) {
            try { costo = new BigDecimal(catalogo.costoDomicilio().trim()); } catch (NumberFormatException e) { costo = new BigDecimal("5000"); }
        }
        final BigDecimal costoFinal = costo.max(BigDecimal.ZERO);

        Long pedidoId;
        try {
            pedidoId = tx.execute(s -> {
                Pedido p = new Pedido();
                p.setNumeroPedido(numero());
                p.setClaveIdempotencia(claveIdem);
                p.setClienteId(clienteId);
                p.setMetodoEntrega(r.metodoEntrega());
                if (r.metodoEntrega() == Pedido.Entrega.DOMICILIO) {
                    p.setDireccionEntrega(r.direccionEntrega().trim()); p.setBarrio(r.barrio()); p.setCiudad(r.ciudad().trim());
                }
                p.setObservacionesEntrega(r.observaciones());
                BigDecimal subtotal = BigDecimal.ZERO;
                for (var e : cantidades.entrySet()) {
                    ProductoCatalogo pc = cat.get(e.getKey());
                    DetallePedido d = new DetallePedido();
                    d.setPedido(p); d.setProductoId(pc.id()); d.setNombreProducto(pc.nombre()); d.setPrecioUnitario(pc.precio()); d.setCantidad(e.getValue());
                    d.setSubtotal(pc.precio().multiply(BigDecimal.valueOf(e.getValue())));
                    subtotal = subtotal.add(d.getSubtotal());
                    p.getDetalles().add(d);
                }
                p.setSubtotal(subtotal); p.setCostoEntrega(costoFinal); p.setTotal(subtotal.add(costoFinal)); p.setEstado(Estado.PENDIENTE);
                p = pedidos.saveAndFlush(p);
                PagoSimulado g = new PagoSimulado();
                g.setPedidoId(p.getId()); g.setReferencia("SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
                g.setImporte(p.getTotal());
                pagos.save(g);
                return p.getId();
            });
        } catch (DataIntegrityViolationException e) {
            Optional<Pedido> existente = pedidos.findByClienteIdAndClaveIdempotencia(clienteId, claveIdem);
            if (existente.isPresent()) return ver(existente.get().getId());
            throw e;
        }

        try {
            catalogo.reservar(pedidoId, cantidades);
        } catch (RuntimeException e) {
            compensarCancelacion(pedidoId, e instanceof ConflictException ? "sin_stock" : "catalogo_no_disponible");
            if (!(e instanceof ConflictException)) {
                try { catalogo.liberar(pedidoId); } catch (RuntimeException ignorado) { log.warn("No se pudo liberar tras fallo de reserva pedidoId={}", pedidoId); }
            }
            throw e;
        }
        registry.counter("cognia_pedidos_creados_total", "metodo", r.metodoEntrega().name()).increment();
        auditoria.registrar("CREAR_PEDIDO", pedidoId, "EXITOSO", null);
        return ver(pedidoId);
    }

    public PedidoDto simularPago(Long id, PagoSimulado.Estado resultado) {
        if (resultado == PagoSimulado.Estado.PENDIENTE) throw new BusinessException("El resultado simulado debe ser APROBADO o RECHAZADO");
        Pedido p = cargarAutorizado(id);
        PagoSimulado g = pagos.findFirstByPedidoIdOrderByIdDesc(id).orElseThrow(() -> new NotFoundException("Pago no encontrado"));
        if (g.getEstado() != PagoSimulado.Estado.PENDIENTE) return ver(id); // idempotente
        if (p.getEstado() != Estado.PENDIENTE) throw new ConflictException("El pedido ya no admite pago");
        if (resultado == PagoSimulado.Estado.APROBADO) {
            tx.executeWithoutResult(s -> {
                PagoSimulado pg = pagos.findById(g.getId()).orElseThrow();
                Pedido pd = pedidos.findById(id).orElseThrow();
                pg.setEstado(PagoSimulado.Estado.APROBADO);
                pd.setEstado(Estado.CONFIRMADO);
            });
            registry.counter("cognia_pagos_total", "estado", "APROBADO").increment();
            registry.counter("cognia_ventas_confirmadas_total").increment();
            registry.counter("cognia_ventas_monto_total").increment(p.getTotal().doubleValue());
            auditoria.registrar("PAGO_SIMULADO_APROBADO", id, "EXITOSO", null);
            try { catalogo.confirmar(id); } catch (RuntimeException e) { log.error("Pedido {} confirmado pero el asiento de salida en inventario fallo; reintentar confirmar", id); }
        } else {
            catalogo.liberar(id); // si falla se propaga 503 y el pedido sigue PENDIENTE (reintentable)
            tx.executeWithoutResult(s -> {
                pagos.findById(g.getId()).orElseThrow().setEstado(PagoSimulado.Estado.RECHAZADO);
                pedidos.findById(id).orElseThrow().setEstado(Estado.CANCELADO);
            });
            registry.counter("cognia_pagos_total", "estado", "RECHAZADO").increment();
            auditoria.registrar("PAGO_SIMULADO_RECHAZADO", id, "EXITOSO", null);
        }
        return ver(id);
    }

    public PedidoDto cancelarCliente(Long id) {
        Pedido p = cargarAutorizado(id);
        if (p.getEstado() != Estado.PENDIENTE && p.getEstado() != Estado.CONFIRMADO) throw new ConflictException("El pedido ya no puede cancelarse");
        return cancelar(id, "CANCELAR_PEDIDO");
    }

    public PedidoDto cambiarEstado(Long id, Estado nuevo) {
        Pedido p = pedidos.findById(id).orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
        if (nuevo == Estado.CANCELADO && (p.getEstado() == Estado.PENDIENTE || TRANSICIONES.getOrDefault(p.getEstado(), Set.of()).contains(Estado.CANCELADO))) {
            return cancelar(id, "CANCELAR_PEDIDO_PERSONAL");
        }
        if (!TRANSICIONES.getOrDefault(p.getEstado(), Set.of()).contains(nuevo)) throw new ConflictException("Transicion no permitida: " + p.getEstado() + " -> " + nuevo);
        if (nuevo == Estado.LISTO_PARA_RECOGIDA && p.getMetodoEntrega() != Pedido.Entrega.RECOGIDA) throw new ConflictException("Solo los pedidos de recogida pasan a LISTO_PARA_RECOGIDA");
        if (nuevo == Estado.EN_CAMINO && p.getMetodoEntrega() != Pedido.Entrega.DOMICILIO) throw new ConflictException("Solo los domicilios pasan a EN_CAMINO");
        tx.executeWithoutResult(s -> pedidos.findById(id).orElseThrow().setEstado(nuevo));
        auditoria.registrar("CAMBIAR_ESTADO_PEDIDO", id, "EXITOSO", p.getEstado() + "->" + nuevo);
        return ver(id);
    }

    private PedidoDto cancelar(Long id, String accion) {
        catalogo.liberar(id);
        tx.executeWithoutResult(s -> {
            pedidos.findById(id).orElseThrow().setEstado(Estado.CANCELADO);
            pagos.findFirstByPedidoIdOrderByIdDesc(id).filter(g -> g.getEstado() == PagoSimulado.Estado.PENDIENTE).ifPresent(g -> g.setEstado(PagoSimulado.Estado.RECHAZADO));
        });
        auditoria.registrar(accion, id, "EXITOSO", null);
        return ver(id);
    }

    private void compensarCancelacion(Long pedidoId, String motivo) {
        tx.executeWithoutResult(s -> {
            pedidos.findById(pedidoId).orElseThrow().setEstado(Estado.CANCELADO);
            pagos.findFirstByPedidoIdOrderByIdDesc(pedidoId).ifPresent(g -> g.setEstado(PagoSimulado.Estado.RECHAZADO));
        });
        registry.counter("cognia_pedidos_compensados_total", "motivo", motivo).increment();
        auditoria.registrar("COMPENSAR_PEDIDO", pedidoId, "FALLIDO", motivo);
    }

    /** Libera reservas de pedidos PENDIENTE sin pago tras 30 minutos. */
    @Scheduled(fixedDelay = 300_000, initialDelay = 120_000)
    public void expirarPendientes() {
        for (Pedido p : pedidos.findByEstadoAndCreadoEnBefore(Estado.PENDIENTE, Instant.now().minus(30, ChronoUnit.MINUTES))) {
            try {
                cancelar(p.getId(), "EXPIRAR_PEDIDO");
                registry.counter("cognia_pedidos_compensados_total", "motivo", "expirado").increment();
            } catch (RuntimeException e) {
                log.warn("No se pudo expirar pedido {}: {}", p.getId(), e.getClass().getSimpleName());
            }
        }
    }

    public PedidoDto ver(Long id) {
        return tx.execute(s -> {
            Pedido p = pedidos.findById(id).orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
            return mapper.toDto(p, pagos.findFirstByPedidoIdOrderByIdDesc(id).orElse(null));
        });
    }

    public PedidoDto verAutorizado(Long id) {
        cargarAutorizado(id);
        return ver(id);
    }

    public PageDto<PedidoDto> misPedidos(int page, int size) {
        return tx.execute(s -> {
            var pg = pedidos.findByClienteId(Actor.id(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("id").descending()));
            return conPagos(pg);
        });
    }

    public PageDto<PedidoDto> buscar(Estado estado, String numero, LocalDate desde, LocalDate hasta, int page, int size) {
        Instant d = (desde == null ? LocalDate.of(2000, 1, 1) : desde).atStartOfDay(ZONA).toInstant();
        Instant h = (hasta == null ? LocalDate.of(2100, 1, 1) : hasta.plusDays(1)).atStartOfDay(ZONA).toInstant();
        return tx.execute(s -> conPagos(pedidos.buscar(estado, blank(numero) ? null : numero.trim(), d, h,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending()))));
    }

    private PageDto<PedidoDto> conPagos(org.springframework.data.domain.Page<Pedido> pg) {
        Map<Long, PagoSimulado> porPedido = new HashMap<>();
        pagos.findByPedidoIdIn(pg.getContent().stream().map(Pedido::getId).toList()).forEach(g -> porPedido.merge(g.getPedidoId(), g, (a, b) -> a.getId() > b.getId() ? a : b));
        return mapper.page(pg, p -> mapper.toDto(p, porPedido.get(p.getId())));
    }

    /** Control de acceso por objeto: un cliente solo ve sus pedidos; para otros devuelve 404 (no revela existencia). */
    private Pedido cargarAutorizado(Long id) {
        Pedido p = pedidos.findById(id).orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
        if (!Actor.esPersonal() && !p.getClienteId().equals(Actor.id())) {
            registry.counter("cognia_security_denied_total").increment();
            throw new NotFoundException("Pedido no encontrado");
        }
        return p;
    }

    private String numero() {
        String n;
        do { n = "PED-" + LocalDate.now(ZONA).format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%06d", random.nextInt(1_000_000)); }
        while (pedidos.existsByNumeroPedido(n));
        return n;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}