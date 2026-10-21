package com.cognia.catalogo.repository;

import com.cognia.catalogo.entity.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByEstadoOrderByNombre(Categoria.Estado estado);
    boolean existsByNombreIgnoreCase(String nombre);
}