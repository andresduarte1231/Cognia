package com.cognia.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cognia.pedidos.dto.Dtos.*;
import com.cognia.pedidos.entity.Pedido;
import com.cognia.pedidos.entity.PagoSimulado;
import com.cognia.pedidos.exception.BusinessException;
import com.cognia.pedidos.exception.ConflictException;
import com.cognia.pedidos.exception.NotFoundException;
import com.cognia.pedidos.mapper.PedidoMapper;
import com.cognia.pedidos.repository.PagoRepository;
import com.cognia.pedidos.repository.PedidoRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class PedidoServiceTest {
    PedidoRepository pedidos = mock(PedidoRepository.class);
    PagoRepository pagos = mock(PagoRepository.class);
    CatalogClient catalogo = mock(CatalogClient.class);
    PedidoMapper mapper = mock(PedidoMapper.class);
    AuditoriaService auditoria = mock(AuditoriaService.class);
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    PedidoService svc;

    @BeforeEach
    void init() {
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        svc = new PedidoService(pedidos, pagos, catalogo, mapper, auditoria, tm, registry);
        autenticar("5", "ROLE_CLIENTE");
        when(pedidos.saveAndFlush(any(Pedido.class))).thenAnswer(i -> { Pedido p = i.getArgument(0); p.setId(100L); return p; });
        when(pagos.save(any(PagoSimulado.class))).thenAnswer(i -> i.getArgument(0));
        Pedido guardado = new Pedido(); guardado.setId(100L); guardado.setClienteId(5L);
        when(pedidos.findById(100L)).thenReturn(Optional.of(guardado));
        when(mapper.toDto(any(Pedido.class), any())).thenReturn(null);
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    static void autenticar(String id, String rol) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(id, "x", rol));
    }

    static ProductoCatalogo prod(long id, String nombre, String precio, int stock) {
        return new ProductoCatalogo(id, nombre, new BigDecimal(precio), stock, "ACTIVO", stock > 0);
    }

    CrearPedidoRequest req(Pedido.Entrega e, ItemRequest... items) {
        return new CrearPedidoRequest(List.of(items), e, e == Pedido.Entrega.DOMICILIO ? "Calle 1 # 2-3" : null, null, e == Pedido.Entrega.DOMICILIO ? "Bogota" : null, null);
    }

    @Test
    void calculaTotalesConPreciosDelCatalogoYCostoDeDomicilio() {
        when(catalogo.productos(any())).thenReturn(Map.of(1L, prod(1, "Arroz", "4500", 10), 2L, prod(2, "Aceite", "12000", 10)));
        when(catalogo.costoDomicilio()).thenReturn("5000");

        svc.crear(req(Pedido.Entrega.DOMICILIO, new ItemRequest(1L, 2), new ItemRequest(2L, 1)), "clave-12345678");

        ArgumentCaptor<Pedido> cap = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).saveAndFlush(cap.capture());
        Pedido p = cap.getValue();
        assertThat(p.getSubtotal()).isEqualByComparingTo("21000");
        assertThat(p.getCostoEntrega()).isEqualByComparingTo("5000");
        assertThat(p.getTotal()).isEqualByComparingTo("26000");
        assertThat(p.getEstado()).isEqualTo(Pedido.Estado.PENDIENTE);
        assertThat(p.getDetalles()).extracting("nombreProducto").containsExactlyInAnyOrder("Arroz", "Aceite");
        verify(catalogo).reservar(eq(100L), any());
    }

    @Test
    void recogidaNoCobraDomicilio() {
        when(catalogo.productos(any())).thenReturn(Map.of(1L, prod(1, "Arroz", "4500", 10)));
        svc.crear(req(Pedido.Entrega.RECOGIDA, new ItemRequest(1L, 1)), "clave-12345678");
        ArgumentCaptor<Pedido> cap = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).saveAndFlush(cap.capture());
        assertThat(cap.getValue().getTotal()).isEqualByComparingTo("4500");
        verify(catalogo, never()).costoDomicilio();
    }

    @Test
    void sumaLineasRepetidasDelMismoProducto() {
        when(catalogo.productos(any())).thenReturn(Map.of(1L, prod(1, "Arroz", "1000", 10)));
        svc.crear(req(Pedido.Entrega.RECOGIDA, new ItemRequest(1L, 2), new ItemRequest(1L, 3)), "clave-12345678");
        ArgumentCaptor<Pedido> cap = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos).saveAndFlush(cap.capture());
        assertThat(cap.getValue().getDetalles()).hasSize(1);
        assertThat(cap.getValue().getTotal()).isEqualByComparingTo("5000");
    }

    @Test
    void rechazaSinClaveDeIdempotencia() {
        assertThatThrownBy(() -> svc.crear(req(Pedido.Entrega.RECOGIDA, new ItemRequest(1L, 1)), null)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(catalogo);
    }

    @Test
    void claveRepetidaDevuelveElPedidoExistenteSinDuplicar() {
        Pedido previo = new Pedido(); previo.setId(100L); previo.setClienteId(5L);
        when(pedidos.findByClienteIdAndClaveIdempotencia(5L, "clave-12345678")).thenReturn(Optional.of(previo));
        svc.crear(req(Pedido.Entrega.RECOGIDA, new ItemRequest(1L, 1)), "clave-12345678");
        verify(pedidos, never()).saveAndFlush(any());
        verifyNoInteractions(catalogo);
    }

    @Test
    void domicilioExigeDireccionYCiudad() {
        var r = new CrearPedidoRequest(List.of(new ItemRequest(1L, 1)), Pedido.Entrega.DOMICILIO, null, null, null, null);
        assertThatThrownBy(() -> svc.crear(r, "clave-12345678")).isInstanceOf(BusinessException.class);
    }

    @Test
    void sinExistenciasNoCreaPedido() {
        when(catalogo.productos(any())).thenReturn(Map.of(1L, prod(1, "Arroz", "4500", 1)));
        assertThatThrownBy(() -> svc.crear(req(Pedido.Entrega.RECOGIDA, new ItemRequest(1L, 5)), "clave-12345678")).isInstanceOf(ConflictException.class);
        verify(pedidos, never()).saveAndFlush(any());
    }

    @Test
    void fallaDeReservaCancelaElPedidoCreado() {
        when(catalogo.productos(any())).thenReturn(Map.of(1L, prod(1, "Arroz", "4500", 10)));
        doThrow(new ConflictException("sin stock")).when(catalogo).reservar(anyLong(), any());
        assertThatThrownBy(() -> svc.crear(req(Pedido.Entrega.RECOGIDA, new ItemRequest(1L, 1)), "clave-12345678")).isInstanceOf(ConflictException.class);
        assertThat(pedidos.findById(100L).orElseThrow().getEstado()).isEqualTo(Pedido.Estado.CANCELADO);
        assertThat(registry.counter("cognia_pedidos_compensados_total", "motivo", "sin_stock").count()).isEqualTo(1.0);
    }

    @Test
    void clienteNoPuedeVerPedidoAjeno() {
        Pedido ajeno = new Pedido(); ajeno.setId(200L); ajeno.setClienteId(99L);
        when(pedidos.findById(200L)).thenReturn(Optional.of(ajeno));
        assertThatThrownBy(() -> svc.verAutorizado(200L)).isInstanceOf(NotFoundException.class);
        assertThat(registry.counter("cognia_security_denied_total").count()).isEqualTo(1.0);
    }

    @Test
    void personalPuedeVerCualquierPedido() {
        autenticar("1", "ROLE_EMPLEADO_VENTAS");
        Pedido ajeno = new Pedido(); ajeno.setId(200L); ajeno.setClienteId(99L);
        when(pedidos.findById(200L)).thenReturn(Optional.of(ajeno));
        svc.verAutorizado(200L);
    }

    @Test
    void transicionesInvalidasSonRechazadas() {
        autenticar("1", "ROLE_ADMINISTRADOR");
        Pedido p = new Pedido(); p.setId(300L); p.setEstado(Pedido.Estado.CONFIRMADO); p.setMetodoEntrega(Pedido.Entrega.DOMICILIO);
        when(pedidos.findById(300L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> svc.cambiarEstado(300L, Pedido.Estado.ENTREGADO)).isInstanceOf(ConflictException.class);
        p.setEstado(Pedido.Estado.EN_PREPARACION);
        assertThatThrownBy(() -> svc.cambiarEstado(300L, Pedido.Estado.LISTO_PARA_RECOGIDA)).isInstanceOf(ConflictException.class);
    }

    @Test
    void pagoRechazadoLiberaInventarioYCancela() {
        Pedido p = new Pedido(); p.setId(100L); p.setClienteId(5L); p.setEstado(Pedido.Estado.PENDIENTE);
        PagoSimulado g = new PagoSimulado(); g.setId(1L); g.setPedidoId(100L); g.setEstado(PagoSimulado.Estado.PENDIENTE);
        when(pedidos.findById(100L)).thenReturn(Optional.of(p));
        when(pagos.findFirstByPedidoIdOrderByIdDesc(100L)).thenReturn(Optional.of(g));
        when(pagos.findById(1L)).thenReturn(Optional.of(g));

        svc.simularPago(100L, PagoSimulado.Estado.RECHAZADO);

        verify(catalogo).liberar(100L);
        assertThat(p.getEstado()).isEqualTo(Pedido.Estado.CANCELADO);
        assertThat(g.getEstado()).isEqualTo(PagoSimulado.Estado.RECHAZADO);
        assertThat(registry.find("cognia_ventas_confirmadas_total").counter()).isNull();
    }

    @Test
    void pagoAprobadoConfirmaYCuentaVenta() {
        Pedido p = new Pedido(); p.setId(100L); p.setClienteId(5L); p.setEstado(Pedido.Estado.PENDIENTE); p.setTotal(new BigDecimal("9000"));
        PagoSimulado g = new PagoSimulado(); g.setId(1L); g.setPedidoId(100L); g.setEstado(PagoSimulado.Estado.PENDIENTE);
        when(pedidos.findById(100L)).thenReturn(Optional.of(p));
        when(pagos.findFirstByPedidoIdOrderByIdDesc(100L)).thenReturn(Optional.of(g));
        when(pagos.findById(1L)).thenReturn(Optional.of(g));

        svc.simularPago(100L, PagoSimulado.Estado.APROBADO);

        assertThat(p.getEstado()).isEqualTo(Pedido.Estado.CONFIRMADO);
        verify(catalogo).confirmar(100L);
        assertThat(registry.counter("cognia_ventas_confirmadas_total").count()).isEqualTo(1.0);
        assertThat(registry.counter("cognia_ventas_monto_total").count()).isEqualTo(9000.0);
    }

    @Test
    void pagoPendienteNoEsUnResultadoValido() {
        assertThatThrownBy(() -> svc.simularPago(100L, PagoSimulado.Estado.PENDIENTE)).isInstanceOf(BusinessException.class);
    }
}