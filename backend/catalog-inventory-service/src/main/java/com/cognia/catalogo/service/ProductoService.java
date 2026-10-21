package com.cognia.catalogo.service;

import com.cognia.catalogo.dto.Dtos.*;
import com.cognia.catalogo.entity.Categoria;
import com.cognia.catalogo.entity.MovimientoInventario;
import com.cognia.catalogo.entity.Producto;
import com.cognia.catalogo.exception.BusinessException;
import com.cognia.catalogo.exception.NotFoundException;
import com.cognia.catalogo.mapper.CatalogoMapper;
import com.cognia.catalogo.repository.CategoriaRepository;
import com.cognia.catalogo.repository.MovimientoRepository;
import com.cognia.catalogo.repository.ProductoRepository;
import com.cognia.catalogo.security.Actor;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {
    private static final Map<String, String> SORTS = Map.of("nombre", "nombre", "precio", "precio", "recientes", "id", "existencias", "existencias");
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final MovimientoRepository movimientos;
    private final CatalogoMapper mapper;
    private final AuditoriaService auditoria;

    public ProductoService(ProductoRepository productos, CategoriaRepository categorias, MovimientoRepository movimientos,
                           CatalogoMapper mapper, AuditoriaService auditoria) {
        this.productos = productos; this.categorias = categorias; this.movimientos = movimientos; this.mapper = mapper; this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PageDto<ProductoDto> buscar(boolean admin, Long categoriaId, String q, boolean promo, String sort, String dir, int page, int size) {
        String campo = SORTS.getOrDefault(sort == null ? "nombre" : sort, "nombre");
        Sort s = Sort.by("desc".equalsIgnoreCase(dir) || "recientes".equals(sort) ? Sort.Direction.DESC : Sort.Direction.ASC, campo);
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), s);
        var estados = admin ? null : EnumSet.of(Producto.Estado.ACTIVO, Producto.Estado.AGOTADO);
        String texto = q == null || q.isBlank() ? null : q.trim();
        return mapper.page(productos.buscar(estados, categoriaId, texto, promo, pageable), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProductoDto obtener(Long id, boolean admin) {
        Producto p = productos.findAllConCategoria(List.of(id)).stream().findFirst().orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        if (!admin && p.getEstado() == Producto.Estado.INACTIVO) throw new NotFoundException("Producto no encontrado");
        return mapper.toDto(p);
    }

    @Transactional(readOnly = true)
    public List<ProductoDto> lote(List<Long> ids) {
        if (ids.isEmpty() || ids.size() > 100) throw new BusinessException("Debe indicar entre 1 y 100 productos");
        return productos.findAllConCategoria(ids).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public ProductoDto crear(ProductoRequest r) {
        Categoria c = categorias.findById(r.categoriaId()).orElseThrow(() -> new NotFoundException("Categoria no encontrada"));
        validarPrecios(r.precio(), r.precioAnterior());
        Producto p = new Producto();
        p.setCategoria(c);
        aplicar(p, r);
        p.setExistencias(0);
        p.setEstado(r.estado() == Producto.Estado.INACTIVO ? Producto.Estado.INACTIVO : Producto.Estado.AGOTADO);
        p = productos.save(p);
        int inicial = r.existencias() == null ? 0 : r.existencias();
        if (inicial > 0) {
            p.setExistencias(inicial);
            normalizar(p);
            MovimientoInventario m = new MovimientoInventario();
            m.setProductoId(p.getId()); m.setUsuarioId(Actor.id()); m.setTipo(MovimientoInventario.Tipo.ENTRADA);
            m.setCantidad(inicial); m.setMotivo("Existencias iniciales");
            movimientos.save(m);
        }
        auditoria.registrar("CREAR_PRODUCTO", "productos", p.getId(), p.getNombre());
        return mapper.toDto(p);
    }

    @Transactional
    public ProductoDto actualizar(Long id, ProductoRequest r) {
        Producto p = productos.bloquearPorIds(List.of(id)).stream().findFirst().orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        Categoria c = categorias.findById(r.categoriaId()).orElseThrow(() -> new NotFoundException("Categoria no encontrada"));
        validarPrecios(r.precio(), r.precioAnterior());
        p.setCategoria(c);
        aplicar(p, r);
        if (r.estado() != null) p.setEstado(r.estado());
        normalizar(p);
        auditoria.registrar("EDITAR_PRODUCTO", "productos", id, p.getNombre());
        return mapper.toDto(productos.save(p));
    }

    @Transactional
    public ProductoDto cambiarPrecio(Long id, PrecioRequest r) {
        validarPrecios(r.precio(), r.precioAnterior());
        Producto p = productos.bloquearPorIds(List.of(id)).stream().findFirst().orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        p.setPrecio(r.precio());
        p.setPrecioAnterior(r.precioAnterior());
        auditoria.registrar("CAMBIAR_PRECIO", "productos", id, "precio=" + r.precio() + " anterior=" + r.precioAnterior());
        return mapper.toDto(productos.save(p));
    }

    @Transactional
    public ProductoDto cambiarEstado(Long id, Producto.Estado estado) {
        Producto p = productos.bloquearPorIds(List.of(id)).stream().findFirst().orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        p.setEstado(estado);
        normalizar(p);
        auditoria.registrar("CAMBIAR_ESTADO_PRODUCTO", "productos", id, estado.name());
        return mapper.toDto(productos.save(p));
    }

    static void normalizar(Producto p) {
        if (p.getEstado() != Producto.Estado.INACTIVO) {
            p.setEstado(p.getExistencias() <= 0 ? Producto.Estado.AGOTADO : Producto.Estado.ACTIVO);
        }
    }

    private void validarPrecios(java.math.BigDecimal precio, java.math.BigDecimal anterior) {
        if (anterior != null && anterior.compareTo(precio) < 0) {
            throw new BusinessException("El precio anterior no puede ser menor al precio actual");
        }
    }

    private void aplicar(Producto p, ProductoRequest r) {
        p.setNombre(r.nombre().trim());
        p.setDescripcion(r.descripcion());
        p.setPrecio(r.precio());
        p.setPrecioAnterior(r.precioAnterior());
        p.setInventarioMinimo(r.inventarioMinimo() == null ? 5 : r.inventarioMinimo());
        p.setImagenUrl(r.imagenUrl() == null ? "/placeholder.svg" : r.imagenUrl());
    }
}