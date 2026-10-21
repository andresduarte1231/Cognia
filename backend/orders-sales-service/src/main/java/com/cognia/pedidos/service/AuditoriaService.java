package com.cognia.pedidos.service;

import com.cognia.pedidos.entity.Auditoria;
import com.cognia.pedidos.repository.AuditoriaRepository;
import com.cognia.pedidos.security.Actor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repo;

    public AuditoriaService(AuditoriaRepository repo) { this.repo = repo; }

    @Transactional
    public void registrar(String accion, Object recursoId, String resultado, String detalle) {
        Auditoria a = new Auditoria();
        a.setUsuarioId(Actor.id());
        a.setAccion(accion);
        a.setRecurso("pedidos");
        a.setRecursoId(recursoId == null ? null : String.valueOf(recursoId));
        a.setResultado(resultado);
        a.setDetalle(detalle);
        a.setCorrelacionId(MDC.get("correlationId"));
        repo.save(a);
    }
}