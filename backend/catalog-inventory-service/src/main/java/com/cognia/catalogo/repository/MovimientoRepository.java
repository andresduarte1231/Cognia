package com.cognia.catalogo.repository;

import com.cognia.catalogo.entity.MovimientoInventario;
import com.cognia.catalogo.entity.MovimientoInventario.Tipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoRepository extends JpaRepository<MovimientoInventario, Long> {
    boolean existsByPedidoIdAndTipo(Long pedidoId, Tipo tipo);

    @Query("select m from MovimientoInventario m where (:productoId is null or m.productoId = :productoId) order by m.id desc")
    Page<MovimientoInventario> listar(@Param("productoId") Long productoId, Pageable pageable);

    java.util.List<MovimientoInventario> findByPedidoIdAndTipo(Long pedidoId, Tipo tipo);
}