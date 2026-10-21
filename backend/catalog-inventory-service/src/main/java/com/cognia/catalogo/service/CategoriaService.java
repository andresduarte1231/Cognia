package com.cognia.catalogo.service;

import com.cognia.catalogo.dto.Dtos.*;
import com.cognia.catalogo.entity.Categoria;
import com.cognia.catalogo.exception.ConflictException;
import com.cognia.catalogo.exception.NotFoundException;
import com.cognia.catalogo.mapper.CatalogoMapper;
import com.cognia.catalogo.repository.CategoriaRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {
    private final CategoriaRepository repo;
    private final CatalogoMapper mapper;
    private final AuditoriaService auditoria;

    public CategoriaService(CategoriaRepository repo, CatalogoMapper mapper, AuditoriaService auditoria) {
        this.repo = repo; this.mapper = mapper; this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDto> listar(boolean admin) {
        List<Categoria> l = admin ? repo.findAll(Sort.by("nombre")) : repo.findByEstadoOrderByNombre(Categoria.Estado.ACTIVA);
        return l.stream().map(mapper::toDto).toList();
    }

    @Transactional
    public CategoriaDto crear(CategoriaRequest r) {
        if (repo.existsByNombreIgnoreCase(r.nombre().trim())) throw new ConflictException("Ya existe una categoria con ese nombre");
        Categoria c = new Categoria();
        c.setNombre(r.nombre().trim());
        c.setDescripcion(r.descripcion());
        if (r.estado() != null) c.setEstado(Categoria.Estado.valueOf(r.estado()));
        c = repo.save(c);
        auditoria.registrar("CREAR_CATEGORIA", "categorias", c.getId(), c.getNombre());
        return mapper.toDto(c);
    }

    @Transactional
    public CategoriaDto actualizar(Long id, CategoriaRequest r) {
        Categoria c = repo.findById(id).orElseThrow(() -> new NotFoundException("Categoria no encontrada"));
        repo.findAll().stream().filter(o -> !o.getId().equals(id) && o.getNombre().equalsIgnoreCase(r.nombre().trim())).findAny()
            .ifPresent(o -> { throw new ConflictException("Ya existe una categoria con ese nombre"); });
        c.setNombre(r.nombre().trim());
        c.setDescripcion(r.descripcion());
        if (r.estado() != null) c.setEstado(Categoria.Estado.valueOf(r.estado()));
        auditoria.registrar("EDITAR_CATEGORIA", "categorias", id, c.getNombre());
        return mapper.toDto(repo.save(c));
    }
}