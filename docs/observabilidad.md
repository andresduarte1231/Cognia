# Observabilidad (fase Operate)

## Instalación (sin Docker)
Binarios en `observability\bin` (ignorado por Git): Prometheus 2.55.1 y Grafana 11.3.0 para Windows. Extraer con `tar -xf` (Expand-Archive se cuelga con estos zips).

## Configuración
- `observability/prometheus/prometheus.yml`: scrape cada 15 s de los 4 servicios en `/actuator/prometheus` con autenticación básica (clave leída de `observability/runtime/metrics.pass`, generada desde `.env`).
- `observability/alerts/cognia-alerts.yml`: 6 reglas (servicio caído, métricas perdidas, 5xx > 5 %, p95 > 1 s, fallo de conexión BD, > 10 autenticaciones fallidas en 5 min).
- `observability/grafana/dashboards`: 6 paneles generados por `generate_dashboards.py` — Estado general, Rendimiento, Disponibilidad, Base de datos, Negocio, Seguridad (solo métricas agregadas, sin datos personales).
- `scripts/prepare` genera los secretos de ejecución (hash bcrypt de Prometheus, datasource de Grafana) en `observability/runtime` (ignorado por Git). Prometheus y Grafana escuchan solo en 127.0.0.1 y exigen autenticación; sin registro ni acceso anónimo.

## Métricas propias
`cognia_db_up`, `cognia_security_{unauthorized,denied}_total`, `cognia_auth_{logins,failures,registros}_total`, `cognia_inventario_reservas_total`, `cognia_pedidos_creados_total`, `cognia_pagos_total`, `cognia_ventas_{confirmadas,monto}_total`, `cognia_pedidos_compensados_total`, `cognia_comunicacion_errores_total`, más las estándar de HTTP, JVM y HikariCP.

## Verificación real realizada
- 4/4 targets en estado `up`; 6/6 reglas de alerta cargadas en estado `ok`; 6/6 paneles aprovisionados y datasource Grafana → Prometheus `OK`.
- Cada consulta de los paneles se evaluó contra Prometheus tras ejecutar `tests/api-smoke.ps1` (34/34): solo devolvieron vacío las series de eventos que no ocurrieron (5xx, compensaciones, errores entre servicios); los *stat* correspondientes muestran 0.
- **Prueba de caída y recuperación** (`tests/operate-outage.ps1`, evidencia en `docs/evidencia-operate-caida-recuperacion.txt`):
  - Caída de `catalog-inventory-service`: el gateway respondió **503** a `/api/productos` y siguió sirviendo la SPA (200).
  - Alerta `ServicioNoDisponible` en **firing a los 35 s**.
  - Reinicio: `up=1` a los **30 s**, `/api/productos` = 200 y la alerta se resolvió.

## Pendiente / límites
- Las alertas se evalúan en Prometheus; no hay Alertmanager ni canal de notificación (solo visibles en Prometheus/Grafana).
- No se capturaron imágenes de los paneles; abrir Grafana para verlos con tráfico.