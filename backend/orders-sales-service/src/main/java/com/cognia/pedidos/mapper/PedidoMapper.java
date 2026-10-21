package com.cognia.pedidos.mapper;

import com.cognia.pedidos.dto.Dtos.*;
import com.cognia.pedidos.entity.*;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PedidoMapper {
    public PagoDto toDto(PagoSimulado g) {
        return g == null ? null : new PagoDto(g.getId(), g.getReferencia(), g.getMetodo(), g.getEstado(), g.getImporte(), g.getPedidoId(), g.getCreadoEn());
    }

    public PedidoDto toDto(Pedido p, PagoSimulado pago) {
        var det = p.getDetalles().stream().map(d -> new DetalleDto(d.getProductoId(), d.getNombreProducto(), d.getPrecioUnitario(), d.getCantidad(), d.getSubtotal())).toList();
        return new PedidoDto(p.getId(), p.getNumeroPedido(), p.getClienteId(), p.getMetodoEntrega(), p.getDireccionEntrega(), p.getBarrio(), p.getCiudad(),
            p.getObservacionesEntrega(), p.getEstado(), p.getSubtotal(), p.getCostoEntrega(), p.getTotal(), p.getCreadoEn(), det, toDto(pago));
    }

    public <E, D> PageDto<D> page(Page<E> p, Function<E, D> f) {
        return new PageDto<>(p.getContent().stream().map(f).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}