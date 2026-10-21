package com.cognia.pedidos.repository;

import com.cognia.pedidos.entity.Pedido;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByClienteIdAndClaveIdempotencia(Long clienteId, String clave);
    boolean existsByNumeroPedido(String numero);
    Page<Pedido> findByClienteId(Long clienteId, Pageable p);
    List<Pedido> findByEstadoAndCreadoEnBefore(Pedido.Estado estado, Instant limite);

    @Query("""
        select p from Pedido p where (:estado is null or p.estado = :estado)
        and (:numero is null or p.numeroPedido like concat('%', :numero, '%'))
        and p.creadoEn >= :desde and p.creadoEn < :hasta
        """)
    Page<Pedido> buscar(@Param("estado") Pedido.Estado estado, @Param("numero") String numero, @Param("desde") Instant desde, @Param("hasta") Instant hasta, Pageable p);

    @Query("select p.estado as estado, count(p) as total from Pedido p group by p.estado")
    List<EstadoConteo> contarPorEstado();

    interface EstadoConteo { Pedido.Estado getEstado(); long getTotal(); }

    @Query(value = """
        select date(p.creado_en) as dia, count(*) as pedidos, coalesce(sum(p.total),0) as total
        from pedidos p join pagos_simulados g on g.pedido_id = p.id
        where g.estado = 'APROBADO' and p.estado <> 'CANCELADO' and p.creado_en >= :desde and p.creado_en < :hasta
        group by date(p.creado_en) order by dia
        """, nativeQuery = true)
    List<VentaDia> ventasPorDia(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    interface VentaDia { java.sql.Date getDia(); long getPedidos(); java.math.BigDecimal getTotal(); }

    @Query(value = """
        select d.nombre_producto as nombre, d.producto_id as productoId, sum(d.cantidad) as unidades, sum(d.subtotal) as ingresos
        from detalle_pedidos d join pedidos p on p.id = d.pedido_id join pagos_simulados g on g.pedido_id = p.id
        where g.estado = 'APROBADO' and p.estado <> 'CANCELADO' and p.creado_en >= :desde and p.creado_en < :hasta
        group by d.producto_id, d.nombre_producto order by unidades desc limit :limite
        """, nativeQuery = true)
    List<MasVendido> masVendidos(@Param("desde") Instant desde, @Param("hasta") Instant hasta, @Param("limite") int limite);

    interface MasVendido { String getNombre(); Long getProductoId(); long getUnidades(); java.math.BigDecimal getIngresos(); }
}