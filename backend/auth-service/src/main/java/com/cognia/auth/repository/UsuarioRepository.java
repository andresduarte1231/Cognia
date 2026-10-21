package com.cognia.auth.repository;

import com.cognia.auth.entity.Usuario;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByRolAndEstado(Usuario.Rol rol, Usuario.Estado estado);

    @Query("""
        select u from Usuario u where (:rol is null or u.rol = :rol) and (:estado is null or u.estado = :estado)
        and (:q is null or lower(u.nombre) like lower(concat('%', :q, '%')) or lower(u.correo) like lower(concat('%', :q, '%')))
        """)
    Page<Usuario> buscar(@Param("rol") Usuario.Rol rol, @Param("estado") Usuario.Estado estado, @Param("q") String q, Pageable p);
}