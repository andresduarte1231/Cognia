package com.cognia.auth.service;

import com.cognia.auth.entity.Auditoria;
import com.cognia.auth.repository.AuditoriaRepository;
import com.cognia.auth.security.Actor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repo;

    public AuditoriaService(AuditoriaRepository repo) { this.repo = repo; }

    @Transactional
    public void registrar(Long usuarioId, String accion, String recurso, Object recursoId, String resultado, String detalle) {
        Auditoria a = new Auditoria();
        a.setUsuarioId(usuarioId != null ? usuarioId : Actor.id());
        a.setAccion(accion);
        a.setRecurso(recurso);
        a.setRecursoId(recursoId == null ? null : String.valueOf(recursoId));
        a.setResultado(resultado);
        a.setDetalle(detalle);
        a.setCorrelacionId(MDC.get("correlationId"));
        repo.save(a);
    }

    /** Para eventos que deben persistir aunque la transaccion llamante se revierta (sin FK a filas no confirmadas). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAparte(String accion, String recurso, String resultado, String detalle) {
        Auditoria a = new Auditoria();
        a.setAccion(accion); a.setRecurso(recurso); a.setResultado(resultado); a.setDetalle(detalle);
        a.setCorrelacionId(MDC.get("correlationId"));
        repo.save(a);
    }

    public void ok(String accion, String recurso, Object recursoId, String detalle) { registrar(null, accion, recurso, recursoId, "EXITOSO", detalle); }
}