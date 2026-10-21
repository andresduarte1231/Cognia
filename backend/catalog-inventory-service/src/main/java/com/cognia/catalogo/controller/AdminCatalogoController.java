package com.cognia.catalogo.controller;

import com.cognia.catalogo.dto.Dtos.*;
import com.cognia.catalogo.service.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Las reglas de acceso (GET para empleados; escritura para propietario/administrador) se aplican en SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminCatalogoController {
    private final ProductoService productos;
    private final CategoriaService categorias;
    private final InventarioService inventario;
    private final ImagenService imagenes;
    private final ConfiguracionService config;

    public AdminCatalogoController(ProductoService productos, CategoriaService categorias, InventarioService inventario, ImagenService imagenes, ConfiguracionService config) {
        this.productos = productos; this.categorias = categorias; this.inventario = inventario; this.imagenes = imagenes; this.config = config;
    }

    @GetMapping("/productos")
    public PageDto<ProductoDto> listar(@RequestParam(required = false) Long categoriaId, @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "nombre") String orden, @RequestParam(defaultValue = "asc") String direccion,
                                       @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return productos.buscar(true, categoriaId, q, false, orden, direccion, pagina, tamano);
    }

    @GetMapping("/productos/{id}")
    public ProductoDto detalle(@PathVariable Long id) { return productos.obtener(id, true); }

    @PostMapping("/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoDto crear(@Valid @RequestBody ProductoRequest r) { return productos.crear(r); }

    @PutMapping("/productos/{id}")
    public ProductoDto editar(@PathVariable Long id, @Valid @RequestBody ProductoRequest r) { return productos.actualizar(id, r); }

    @PatchMapping("/productos/{id}/precio")
    public ProductoDto precio(@PathVariable Long id, @Valid @RequestBody PrecioRequest r) { return productos.cambiarPrecio(id, r); }

    @PatchMapping("/productos/{id}/estado")
    public ProductoDto estado(@PathVariable Long id, @Valid @RequestBody EstadoProductoRequest r) { return productos.cambiarEstado(id, r.estado()); }

    @PostMapping(value = "/productos/{id}/imagen", consumes = "multipart/form-data")
    public ProductoDto imagen(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) throws IOException { return imagenes.subir(id, archivo); }

    @GetMapping("/categorias")
    public List<CategoriaDto> categorias() { return categorias.listar(true); }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaDto crearCategoria(@Valid @RequestBody CategoriaRequest r) { return categorias.crear(r); }

    @PutMapping("/categorias/{id}")
    public CategoriaDto editarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaRequest r) { return categorias.actualizar(id, r); }

    @GetMapping("/inventario/resumen")
    public ResumenInventario resumen() { return inventario.resumen(); }

    @GetMapping("/inventario/alertas")
    public List<ProductoDto> alertas() { return inventario.alertas(); }

    @GetMapping("/inventario/movimientos")
    public PageDto<MovimientoDto> movimientos(@RequestParam(required = false) Long productoId, @RequestParam(defaultValue = "0") int pagina,
                                              @RequestParam(defaultValue = "20") int tamano) {
        return inventario.listar(productoId, pagina, tamano);
    }

    @PostMapping("/inventario/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoDto movimiento(@Valid @RequestBody MovimientoRequest r) { return inventario.movimientoManual(r); }

    @PutMapping("/configuracion")
    public Map<String, String> configuracion(@Valid @RequestBody ConfiguracionRequest r) { return config.actualizar(r.valores()); }
}