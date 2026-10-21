package com.cognia.catalogo.controller;

import com.cognia.catalogo.dto.Dtos.*;
import com.cognia.catalogo.service.CategoriaService;
import com.cognia.catalogo.service.ConfiguracionService;
import com.cognia.catalogo.service.ProductoService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class CatalogoPublicController {
    private final ProductoService productos;
    private final CategoriaService categorias;
    private final ConfiguracionService config;

    public CatalogoPublicController(ProductoService productos, CategoriaService categorias, ConfiguracionService config) {
        this.productos = productos; this.categorias = categorias; this.config = config;
    }

    @GetMapping("/api/productos")
    public PageDto<ProductoDto> listar(@RequestParam(required = false) Long categoriaId, @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "false") boolean promocion, @RequestParam(defaultValue = "nombre") String orden,
                                       @RequestParam(defaultValue = "asc") String direccion, @RequestParam(defaultValue = "0") int pagina,
                                       @RequestParam(defaultValue = "12") int tamano) {
        return productos.buscar(false, categoriaId, q, promocion, orden, direccion, pagina, tamano);
    }

    @GetMapping("/api/productos/lote")
    public List<ProductoDto> lote(@RequestParam List<Long> ids) { return productos.lote(ids); }

    @GetMapping("/api/productos/{id}")
    public ProductoDto detalle(@PathVariable Long id) { return productos.obtener(id, false); }

    @GetMapping("/api/categorias")
    public List<CategoriaDto> categorias() { return categorias.listar(false); }

    @GetMapping("/api/configuracion/publica")
    public Map<String, String> configuracion() { return config.publica(); }
}