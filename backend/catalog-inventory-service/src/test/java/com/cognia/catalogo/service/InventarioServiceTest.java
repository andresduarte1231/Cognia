package com.cognia.catalogo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cognia.catalogo.dto.Dtos.*;
import com.cognia.catalogo.entity.MovimientoInventario;
import com.cognia.catalogo.entity.MovimientoInventario.Tipo;
import com.cognia.catalogo.entity.Producto;
import com.cognia.catalogo.exception.BusinessException;
import com.cognia.catalogo.exception.ConflictException;
import com.cognia.catalogo.mapper.CatalogoMapper;
import com.cognia.catalogo.repository.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventarioServiceTest {
    ProductoRepository productos = mock(ProductoRepository.class);
    MovimientoRepository movimientos = mock(MovimientoRepository.class);
    CategoriaRepository categorias = mock(CategoriaRepository.class);
    CatalogoMapper mapper = mock(CatalogoMapper.class);
    AuditoriaService auditoria = mock(AuditoriaService.class);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    InventarioService svc;
    Producto p;

    @BeforeEach
    void init() {
        svc = new InventarioService(productos, movimientos, categorias, mapper, auditoria, registry);
        p = new Producto(); p.setId(1L); p.setNombre("Arroz"); p.setExistencias(10);
        when(productos.bloquearPorIds(any())).thenReturn(List.of(p));
        when(movimientos.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void reservaDescuentaExistenciasYRegistraMovimiento() {
        svc.reservar(new ReservaRequest(50L, List.of(new ItemReserva(1L, 4))));
        assertThat(p.getExistencias()).isEqualTo(6);
        verify(movimientos).save(argThat(m -> m.getTipo() == Tipo.RESERVA && m.getCantidad() == 4 && m.getPedidoId() == 50L));
        assertThat(registry.counter("cognia_inventario_reservas_total", "resultado", "ok").count()).isEqualTo(1.0);
    }

    @Test
    void reservaMayorAlStockFallaSinModificarNada() {
        assertThatThrownBy(() -> svc.reservar(new ReservaRequest(50L, List.of(new ItemReserva(1L, 11))))).isInstanceOf(ConflictException.class);
        assertThat(p.getExistencias()).isEqualTo(10);
        verify(movimientos, never()).save(any());
        assertThat(registry.counter("cognia_inventario_reservas_total", "resultado", "sin_stock").count()).isEqualTo(1.0);
    }

    @Test
    void reservarUnaSegundaVezElMismoPedidoEsIdempotente() {
        when(movimientos.existsByPedidoIdAndTipo(50L, Tipo.RESERVA)).thenReturn(true);
        svc.reservar(new ReservaRequest(50L, List.of(new ItemReserva(1L, 4))));
        assertThat(p.getExistencias()).isEqualTo(10);
        verify(movimientos, never()).save(any());
    }

    @Test
    void reservaTotalMarcaElProductoComoAgotado() {
        svc.reservar(new ReservaRequest(50L, List.of(new ItemReserva(1L, 10))));
        assertThat(p.getExistencias()).isZero();
        assertThat(p.getEstado()).isEqualTo(Producto.Estado.AGOTADO);
    }

    @Test
    void liberarRestauraExistenciasUnaSolaVez() {
        MovimientoInventario r = new MovimientoInventario(); r.setProductoId(1L); r.setPedidoId(50L); r.setTipo(Tipo.RESERVA); r.setCantidad(4);
        when(movimientos.findByPedidoIdAndTipo(50L, Tipo.RESERVA)).thenReturn(List.of(r));
        p.setExistencias(6);
        svc.liberar(50L);
        assertThat(p.getExistencias()).isEqualTo(10);
        when(movimientos.existsByPedidoIdAndTipo(50L, Tipo.LIBERACION)).thenReturn(true);
        svc.liberar(50L);
        assertThat(p.getExistencias()).isEqualTo(10);
    }

    @Test
    void salidaManualNoPuedeDejarInventarioNegativo() {
        assertThatThrownBy(() -> svc.movimientoManual(new MovimientoRequest(1L, Tipo.SALIDA, 11, "merma"))).isInstanceOf(ConflictException.class);
        assertThat(p.getExistencias()).isEqualTo(10);
    }

    @Test
    void entradaYAjusteActualizanExistencias() {
        svc.movimientoManual(new MovimientoRequest(1L, Tipo.ENTRADA, 5, "compra"));
        assertThat(p.getExistencias()).isEqualTo(15);
        svc.movimientoManual(new MovimientoRequest(1L, Tipo.AJUSTE, 3, "conteo fisico"));
        assertThat(p.getExistencias()).isEqualTo(3);
    }

    @Test
    void ajusteSinCambioYReservasManualesSeRechazan() {
        assertThatThrownBy(() -> svc.movimientoManual(new MovimientoRequest(1L, Tipo.AJUSTE, 10, "igual"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> svc.movimientoManual(new MovimientoRequest(1L, Tipo.RESERVA, 1, "x"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> svc.movimientoManual(new MovimientoRequest(1L, Tipo.ENTRADA, 0, "x"))).isInstanceOf(BusinessException.class);
    }

    @Test
    void confirmarSoloRegistraSalidaSinDescontarDeNuevo() {
        MovimientoInventario r = new MovimientoInventario(); r.setProductoId(1L); r.setPedidoId(50L); r.setTipo(Tipo.RESERVA); r.setCantidad(4);
        when(movimientos.findByPedidoIdAndTipo(50L, Tipo.RESERVA)).thenReturn(List.of(r));
        p.setExistencias(6);
        svc.confirmar(50L);
        assertThat(p.getExistencias()).isEqualTo(6);
        verify(movimientos).save(argThat(m -> m.getTipo() == Tipo.SALIDA && m.getCantidad() == 4));
    }
}