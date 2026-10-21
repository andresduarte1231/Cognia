package com.cognia.catalogo.service;

import com.cognia.catalogo.dto.Dtos.ProductoDto;
import com.cognia.catalogo.entity.Producto;
import com.cognia.catalogo.exception.BusinessException;
import com.cognia.catalogo.exception.NotFoundException;
import com.cognia.catalogo.mapper.CatalogoMapper;
import com.cognia.catalogo.repository.ProductoRepository;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Carga segura: limite de tamano, tipo verificado por firma binaria (no por nombre/Content-Type), nombre aleatorio. */
@Service
public class ImagenService {
    private static final long MAX = 2L * 1024 * 1024;
    private final Path dir;
    private final ProductoRepository productos;
    private final CatalogoMapper mapper;
    private final AuditoriaService auditoria;

    public ImagenService(@Value("${cognia.uploads-dir}") String dir, ProductoRepository productos, CatalogoMapper mapper, AuditoriaService auditoria) throws IOException {
        this.dir = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.dir);
        this.productos = productos; this.mapper = mapper; this.auditoria = auditoria;
    }

    public Path directorio() { return dir; }

    static String extension(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }

    @Transactional
    public ProductoDto subir(Long productoId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new BusinessException("Archivo vacio");
        if (file.getSize() > MAX) throw new BusinessException("La imagen supera 2 MB");
        byte[] data = file.getBytes();
        String ext = extension(data);
        if (ext == null) throw new BusinessException("Formato no permitido (solo JPG, PNG o WEBP)");
        Producto p = productos.findById(productoId).orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        String nombre = UUID.randomUUID() + "." + ext;
        Files.write(dir.resolve(nombre), data, StandardOpenOption.CREATE_NEW);
        p.setImagenUrl("/uploads/" + nombre);
        auditoria.registrar("SUBIR_IMAGEN_PRODUCTO", "productos", productoId, nombre);
        return mapper.toDto(productos.save(p));
    }
}