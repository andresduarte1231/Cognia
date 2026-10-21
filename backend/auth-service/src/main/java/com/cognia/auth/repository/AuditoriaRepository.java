package com.cognia.auth.repository;

import com.cognia.auth.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    @Query("select a from Auditoria a where (:accion is null or a.accion = :accion) order by a.id desc")
    Page<Auditoria> listar(@Param("accion") String accion, Pageable p);
}