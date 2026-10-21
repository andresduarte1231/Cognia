package com.cognia.catalogo.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/** Gauge cognia_db_up: 1 si la base responde a una validacion de conexion, 0 en caso contrario. */
@Component
public class DbUpMetric {
    public DbUpMetric(MeterRegistry registry, DataSource ds) {
        Gauge.builder("cognia_db_up", () -> {
            try (Connection c = ds.getConnection()) {
                return c.isValid(2) ? 1.0 : 0.0;
            } catch (Exception e) {
                return 0.0;
            }
        }).description("Estado de conexion con MySQL/MariaDB").register(registry);
    }
}
