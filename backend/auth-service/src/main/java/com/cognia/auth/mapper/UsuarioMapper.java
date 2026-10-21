package com.cognia.auth.mapper;

import com.cognia.auth.dto.Dtos.*;
import com.cognia.auth.entity.Auditoria;
import com.cognia.auth.entity.Usuario;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {
    public UsuarioDto toDto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.getEstado(), u.getCreadoEn());
    }

    public AuditoriaDto toDto(Auditoria a) {
        return new AuditoriaDto(a.getId(), a.getUsuarioId(), a.getAccion(), a.getRecurso(), a.getRecursoId(), a.getResultado(), a.getDetalle(), a.getCorrelacionId(), a.getCreadoEn());
    }

    public <E, D> PageDto<D> page(Page<E> p, Function<E, D> f) {
        return new PageDto<>(p.getContent().stream().map(f).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}