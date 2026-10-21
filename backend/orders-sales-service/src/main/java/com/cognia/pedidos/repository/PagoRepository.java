package com.cognia.pedidos.repository;

import com.cognia.pedidos.entity.PagoSimulado;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<PagoSimulado, Long> {
    Optional<PagoSimulado> findFirstByPedidoIdOrderByIdDesc(Long pedidoId);
    List<PagoSimulado> findByPedidoIdIn(java.util.Collection<Long> ids);

    @Query("select g from PagoSimulado g where (:estado is null or g.estado = :estado) and g.creadoEn >= :desde and g.creadoEn < :hasta order by g.id desc")
    Page<PagoSimulado> buscar(@Param("estado") PagoSimulado.Estado estado, @Param("desde") Instant desde, @Param("hasta") Instant hasta, Pageable p);

    @Query("select g.estado as estado, count(g) as total, coalesce(sum(g.importe),0) as importe from PagoSimulado g group by g.estado")
    List<PagoResumen> resumen();

    interface PagoResumen { PagoSimulado.Estado getEstado(); long getTotal(); java.math.BigDecimal getImporte(); }
}