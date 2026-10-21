package com.cognia.catalogo.service;

import com.cognia.catalogo.entity.ConfiguracionTienda;
import com.cognia.catalogo.exception.BusinessException;
import com.cognia.catalogo.repository.ConfiguracionRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionService {
    private static final Set<String> EDITABLES = Set.of("mision", "vision", "valores", "costo_domicilio", "contacto_telefono", "contacto_correo",
        "contacto_direccion", "info_domicilios", "info_recogida", "promociones_texto", "domicilio_habilitado", "recogida_habilitada");
    private static final Set<String> PUBLICAS;
    static {
        Set<String> s = new HashSet<>(EDITABLES);
        s.addAll(Set.of("nombre_comercial", "moneda"));
        PUBLICAS = Set.copyOf(s);
    }
    private final ConfiguracionRepository repo;
    private final AuditoriaService auditoria;

    public ConfiguracionService(ConfiguracionRepository repo, AuditoriaService auditoria) { this.repo = repo; this.auditoria = auditoria; }

    @Transactional(readOnly = true)
    public Map<String, String> publica() {
        Map<String, String> m = new TreeMap<>();
        repo.findAll().stream().filter(c -> PUBLICAS.contains(c.getClave())).forEach(c -> m.put(c.getClave(), c.getValor() == null ? "" : c.getValor()));
        return m;
    }

    @Transactional
    public Map<String, String> actualizar(Map<String, String> valores) {
        for (String k : valores.keySet()) if (!EDITABLES.contains(k)) throw new BusinessException("Clave no editable: " + k);
        valores.forEach((k, v) -> {
            ConfiguracionTienda c = repo.findByClave(k).orElseGet(() -> { ConfiguracionTienda n = new ConfiguracionTienda(); n.setClave(k); return n; });
            c.setValor(v);
            repo.save(c);
        });
        auditoria.registrar("ACTUALIZAR_CONFIGURACION", "configuracion_tienda", null, String.join(",", valores.keySet()));
        return publica();
    }
}