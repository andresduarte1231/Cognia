package com.cognia.catalogo.mapper;

import com.cognia.catalogo.dto.Dtos.*;
import com.cognia.catalogo.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import java.util.function.Function;

@Component
public class CatalogoMapper {

    public ProductoDto toDto(Producto p) {
        boolean promo = p.getPrecioAnterior() != null && p.getPrecioAnterior().compareTo(p.getPrecio()) > 0;
        boolean disp = p.getEstado() == Producto.Estado.ACTIVO && p.getExistencias() > 0;
        return new ProductoDto(p.getId(), p.getCategoria().getId(), p.getCategoria().getNombre(), p.getNombre(), p.getDescripcion(),
            p.getPrecio(), p.getPrecioAnterior(), promo, p.getExistencias(), p.getInventarioMinimo(), p.getImagenUrl(), p.getEstado(), disp);
    }

    public CategoriaDto toDto(Categoria c) {
        return new CategoriaDto(c.getId(), c.getNombre(), c.getDescripcion(), c.getEstado().name());
    }

    public MovimientoDto toDto(MovimientoInventario m) {
        return new MovimientoDto(m.getId(), m.getProductoId(), m.getUsuarioId(), m.getPedidoId(), m.getTipo(), m.getCantidad(), m.getMotivo(), m.getCreadoEn());
    }

    public <E, D> PageDto<D> page(Page<E> p, Function<E, D> f) {
        return new PageDto<>(p.getContent().stream().map(f).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}