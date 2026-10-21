# Arquitectura

```
Navegador ──► API Gateway :4000 (sirve React, CORS, CSP, correlación, timeouts)
                 ├─► auth-service        :4001  /api/auth, /api/admin/usuarios, /api/admin/auditoria
                 ├─► catalog-inventory   :4002  /api/productos, /api/categorias, /api/configuracion, /api/admin/{productos,categorias,inventario,configuracion}, /uploads
                 └─► orders-sales        :4003  /api/pedidos, /api/admin/{pedidos,pagos,reportes}
                              │ (REST interno /internal con clave de servicio)
                              ▼
                        MariaDB cognia_aqui_es_tona   (Prometheus ◄── /actuator/prometheus de los 4 procesos ──► Grafana)
```

## MVC en cada servicio
- **Modelo**: entidades JPA (`model/`) y repositorios Spring Data.
- **Vista**: representaciones JSON (DTO) consumidas por React.
- **Controlador**: `@RestController` por recurso; reglas en `service/` con `@Transactional`.

## Decisiones relevantes
- **Saga de pedidos** (orders ↔ catalog): reserva de inventario con bloqueo pesimista; si falla un paso se compensa (liberación). `Idempotency-Key` obligatorio al crear pedidos. Un proceso programado expira pedidos PENDIENTE tras 30 min.
- **Inventario**: RESERVA descuenta, LIBERACION restituye, SALIDA solo registra, AJUSTE fija valor absoluto.
- **Seguridad**: JWT HS256 (60 min), BCrypt, roles PROPIETARIO / ADMINISTRADOR / EMPLEADO_VENTAS / CLIENTE.
- **Esquema**: existente, sin recrear; cambios mediante migraciones SQL incrementales en `database/migrations` (se usó SQL versionado en lugar de Flyway por compatibilidad con MariaDB 10.4 y el esquema preexistente; Hibernate en modo `validate`).
- **Driver**: MariaDB JDBC (compatible con XAMPP).
- **Versiones**: el gateway requiere Boot 3.4.1 + Spring Cloud 2024.0.0; con Cloud 2023.0.x sobre Boot 3.3.x fallaba (`NoSuchMethodError HttpHeaders.headerSet`).
- **Frontend en Spring Boot**: `vite build` escribe en `api-gateway/src/main/resources/static`; `SpaFallbackFilter` devuelve `index.html` para rutas de React.