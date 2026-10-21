package com.cognia.catalogo.service;

import com.cognia.catalogo.entity.Auditoria;
import com.cognia.catalogo.repository.AuditoriaRepository;
import com.cognia.catalogo.security.Actor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repo;

    public AuditoriaService(AuditoriaRepository repo) { this.repo = repo; }

    public void registrar(String accion, String recurso, Object recursoId, String detalle) {
        Auditoria a = new Auditoria();
        a.setUsuarioId(Actor.id());
        a.setAccion(accion);
        a.setRecurso(recurso);
        a.setRecursoId(recursoId == null ? null : String.valueOf(recursoId));
        a.setDetalle(detalle);
        a.setCorrelacionId(MDC.get("correlationId"));
        repo.save(a);
    }
}