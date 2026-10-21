package com.cognia.catalogo.repository;

import com.cognia.catalogo.entity.Producto;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
        select p from Producto p join fetch p.categoria c
        where (:estados is null or p.estado in :estados)
          and (:categoriaId is null or c.id = :categoriaId)
          and (:q is null or lower(p.nombre) like lower(concat('%', :q, '%')))
          and (:promo = false or (p.precioAnterior is not null and p.precioAnterior > p.precio))
        """)
    Page<Producto> buscar(@Param("estados") Collection<Producto.Estado> estados, @Param("categoriaId") Long categoriaId,
                          @Param("q") String q, @Param("promo") boolean promo, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids order by p.id")
    List<Producto> bloquearPorIds(@Param("ids") Collection<Long> ids);

    @Query("select p from Producto p join fetch p.categoria where p.id in :ids")
    List<Producto> findAllConCategoria(@Param("ids") Collection<Long> ids);

    long countByEstado(Producto.Estado estado);

    @Query("select count(p) from Producto p where p.estado <> 'INACTIVO' and p.existencias <= p.inventarioMinimo and p.existencias > 0")
    long contarBajos();

    @Query("select p from Producto p join fetch p.categoria where p.estado <> 'INACTIVO' and p.existencias <= p.inventarioMinimo order by p.existencias")
    List<Producto> alertasBajas();

    long countByCategoriaId(Long categoriaId);
}