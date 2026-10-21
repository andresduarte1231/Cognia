"""Genera los paneles JSON de Grafana de COGNIA. Uso: python generate_dashboards.py
Los JSON resultantes se versionan; este script solo facilita mantenerlos."""
import json
import pathlib

OUT = pathlib.Path(__file__).parent / "dashboards"
DS = {"type": "prometheus", "uid": "cognia-prometheus"}
_id = 0


def _next():
    global _id
    _id += 1
    return _id


def _targets(exprs):
    return [{"datasource": DS, "expr": e, "legendFormat": l, "refId": chr(65 + i)} for i, (e, l) in enumerate(exprs)]


def stat(title, expr, x, y, w=4, h=4, unit="short", legend="", thresholds=None, mappings=None, decimals=None):
    steps = thresholds or [{"color": "green", "value": None}]
    d = {"defaults": {"unit": unit, "thresholds": {"mode": "absolute", "steps": steps}, "mappings": mappings or []}, "overrides": []}
    if decimals is not None:
        d["defaults"]["decimals"] = decimals
    return {"id": _next(), "type": "stat", "title": title, "datasource": DS, "gridPos": {"x": x, "y": y, "w": w, "h": h},
            "targets": _targets([(expr, legend)]), "fieldConfig": d,
            "options": {"reduceOptions": {"calcs": ["lastNotNull"]}, "colorMode": "background", "graphMode": "area", "textMode": "value_and_name" if legend else "auto"}}


def ts(title, exprs, x, y, w=12, h=8, unit="short", stack=False):
    custom = {"drawStyle": "line", "lineWidth": 2, "fillOpacity": 15, "showPoints": "never"}
    if stack:
        custom["stacking"] = {"mode": "normal"}
    return {"id": _next(), "type": "timeseries", "title": title, "datasource": DS, "gridPos": {"x": x, "y": y, "w": w, "h": h},
            "targets": _targets(exprs), "fieldConfig": {"defaults": {"unit": unit, "custom": custom}, "overrides": []},
            "options": {"legend": {"displayMode": "list", "placement": "bottom"}, "tooltip": {"mode": "multi"}}}


def state_timeline(title, expr, legend, x, y, w=24, h=7):
    maps = [{"type": "value", "options": {"0": {"text": "CAIDO", "color": "red"}, "1": {"text": "ACTIVO", "color": "green"}}}]
    return {"id": _next(), "type": "state-timeline", "title": title, "datasource": DS, "gridPos": {"x": x, "y": y, "w": w, "h": h},
            "targets": _targets([(expr, legend)]), "fieldConfig": {"defaults": {"mappings": maps, "custom": {"fillOpacity": 80}}, "overrides": []},
            "options": {"showValue": "never", "legend": {"displayMode": "list", "placement": "bottom"}}}


def heatmap(title, expr, x, y, w=24, h=8):
    t = _targets([(expr, "{{le}}")])
    t[0]["format"] = "heatmap"
    return {"id": _next(), "type": "heatmap", "title": title, "datasource": DS, "gridPos": {"x": x, "y": y, "w": w, "h": h},
            "targets": t, "options": {"calculate": False, "yAxis": {"unit": "s"}, "color": {"scheme": "Oranges"}}}


def dashboard(uid, title, panels, tags=None):
    return {"uid": uid, "title": title, "tags": ["cognia"] + (tags or []), "timezone": "browser", "schemaVersion": 39, "version": 1,
            "refresh": "10s", "time": {"from": "now-30m", "to": "now"}, "editable": True, "panels": panels,
            "templating": {"list": []}, "annotations": {"list": []}}


SVC = 'job=~"api-gateway|auth-service|catalog-inventory-service|orders-sales-service"'
REQ = 'http_server_requests_seconds'
NOACT = 'uri!~"/actuator.*"'
RED_GREEN = [{"color": "red", "value": None}, {"color": "green", "value": 1}]
UPDOWN = [{"type": "value", "options": {"0": {"text": "CAIDO"}, "1": {"text": "ACTIVO"}}}]
LAT = [{"color": "green", "value": None}, {"color": "orange", "value": 0.5}, {"color": "red", "value": 1}]
ERR = [{"color": "green", "value": None}, {"color": "orange", "value": 0.01}, {"color": "red", "value": 0.05}]

dashboards = {}

# 1. Estado general
p = []
for i, (job, nombre) in enumerate([("api-gateway", "API Gateway"), ("auth-service", "Usuarios/Auth"), ("catalog-inventory-service", "Catalogo/Inventario"), ("orders-sales-service", "Pedidos/Ventas")]):
    p.append(stat(nombre, f'up{{job="{job}"}}', i * 6, 0, w=6, h=3, thresholds=RED_GREEN, mappings=UPDOWN))
p += [
    stat("Solicitudes/s", f'sum(rate({REQ}_count{{{NOACT}}}[1m]))', 0, 3, w=6, unit="reqps", decimals=2),
    stat("% errores 5xx (5 min)", f'sum(rate({REQ}_count{{status=~"5..",{NOACT}}}[5m])) / clamp_min(sum(rate({REQ}_count{{{NOACT}}}[5m])), 0.0001) or vector(0)', 6, 3, w=6, unit="percentunit", thresholds=ERR),
    stat("Latencia p95 (5 min)", f'histogram_quantile(0.95, sum by (le) (rate({REQ}_bucket{{{NOACT}}}[5m])))', 12, 3, w=6, unit="s", thresholds=LAT, decimals=3),
    stat("Alertas activas", 'count(ALERTS{alertstate="firing"}) or vector(0)', 18, 3, w=6, thresholds=[{"color": "green", "value": None}, {"color": "red", "value": 1}]),
    ts("Solicitudes por segundo por servicio", [(f'sum by (application) (rate({REQ}_count{{{NOACT}}}[1m]))', "{{application}}")], 0, 7, unit="reqps"),
    ts("Memoria heap usada", [('sum by (application) (jvm_memory_used_bytes{area="heap"})', "{{application}}")], 12, 7, unit="bytes"),
    ts("CPU del proceso", [('process_cpu_usage', "{{application}}")], 0, 15, unit="percentunit"),
    ts("Hilos JVM activos", [('jvm_threads_live_threads', "{{application}}")], 12, 15),
]
dashboards["cognia-01-estado-general"] = dashboard("cognia-estado", "COGNIA 1 - Estado general", p, ["operate"])

# 2. Rendimiento
p = [
    ts("Solicitudes por segundo por servicio", [(f'sum by (application) (rate({REQ}_count{{{NOACT}}}[1m]))', "{{application}}")], 0, 0, unit="reqps"),
    ts("Latencia p50 / p95 / p99 (todos los servicios)", [
        (f'histogram_quantile(0.5, sum by (le) (rate({REQ}_bucket{{{NOACT}}}[5m])))', "p50"),
        (f'histogram_quantile(0.95, sum by (le) (rate({REQ}_bucket{{{NOACT}}}[5m])))', "p95"),
        (f'histogram_quantile(0.99, sum by (le) (rate({REQ}_bucket{{{NOACT}}}[5m])))', "p99")], 12, 0, unit="s"),
    ts("Latencia p95 por servicio", [(f'histogram_quantile(0.95, sum by (application, le) (rate({REQ}_bucket{{{NOACT}}}[5m])))', "{{application}}")], 0, 8, unit="s"),
    ts("Respuestas por clase de estado", [(f'sum by (outcome) (rate({REQ}_count{{{NOACT}}}[1m]))', "{{outcome}}")], 12, 8, unit="reqps", stack=True),
    ts("Errores 5xx por servicio", [(f'sum by (application) (rate({REQ}_count{{status=~"5..",{NOACT}}}[1m]))', "{{application}}")], 0, 16, unit="reqps"),
    ts("Endpoints mas lentos (p95 por URI)", [(f'topk(8, histogram_quantile(0.95, sum by (application, uri, le) (rate({REQ}_bucket{{{NOACT}}}[5m]))))', "{{application}} {{uri}}")], 12, 16, unit="s"),
    heatmap("Distribucion de duracion de solicitudes", f'sum by (le) (increase({REQ}_bucket{{{NOACT}}}[1m]))', 0, 24),
]
dashboards["cognia-02-rendimiento"] = dashboard("cognia-rendimiento", "COGNIA 2 - Rendimiento", p, ["operate"])

# 3. Disponibilidad
p = [
    state_timeline("Disponibilidad por servicio", f'up{{{SVC}}}', "{{job}}", 0, 0),
]
for i, job in enumerate(["api-gateway", "auth-service", "catalog-inventory-service", "orders-sales-service"]):
    p.append(stat(f"Disponibilidad 1h - {job}", f'avg_over_time(up{{job="{job}"}}[1h])', i * 6, 7, w=6, h=4, unit="percentunit", thresholds=[{"color": "red", "value": None}, {"color": "orange", "value": 0.95}, {"color": "green", "value": 0.99}], decimals=2))
p += [
    ts("Tiempo en linea (uptime)", [('process_uptime_seconds', "{{application}}")], 0, 11, unit="s"),
    ts("Duracion del scrape", [('scrape_duration_seconds', "{{job}}")], 12, 11, unit="s"),
    ts("Errores de comunicacion entre servicios", [('sum by (application, destino, tipo) (rate(cognia_comunicacion_errores_total[5m]))', "{{application}} -> {{destino}} ({{tipo}})")], 0, 19),
    ts("Respuestas 503/504 por servicio", [(f'sum by (application, status) (rate({REQ}_count{{status=~"503|504"}}[5m]))', "{{application}} {{status}}")], 12, 19, unit="reqps"),
]
dashboards["cognia-03-disponibilidad"] = dashboard("cognia-disponibilidad", "COGNIA 3 - Disponibilidad de microservicios", p, ["operate"])

# 4. Base de datos
p = []
for i, (app, n) in enumerate([("auth-service", "Auth"), ("catalog-inventory-service", "Catalogo"), ("orders-sales-service", "Pedidos")]):
    p.append(stat(f"Conexion MySQL - {n}", f'cognia_db_up{{application="{app}"}}', i * 8, 0, w=8, h=4, thresholds=RED_GREEN, mappings=[{"type": "value", "options": {"0": {"text": "SIN CONEXION"}, "1": {"text": "CONECTADO"}}}]))
p += [
    ts("Conexiones activas del pool", [('hikaricp_connections_active', "{{application}}")], 0, 4),
    ts("Conexiones inactivas / pendientes", [('hikaricp_connections_idle', "inactivas {{application}}"), ('hikaricp_connections_pending', "pendientes {{application}}")], 12, 4),
    ts("Tiempo de obtencion de conexion (media)", [('rate(hikaricp_connections_acquire_seconds_sum[5m]) / clamp_min(rate(hikaricp_connections_acquire_seconds_count[5m]), 0.0001)', "{{application}}")], 0, 12, unit="s"),
    ts("Tiempo de uso de conexion (media)", [('rate(hikaricp_connections_usage_seconds_sum[5m]) / clamp_min(rate(hikaricp_connections_usage_seconds_count[5m]), 0.0001)', "{{application}}")], 12, 12, unit="s"),
    ts("Tiempos de espera agotados del pool", [('increase(hikaricp_connections_timeout_total[5m])', "{{application}}")], 0, 20),
    ts("Conexiones maximas configuradas", [('hikaricp_connections_max', "{{application}}")], 12, 20),
]
dashboards["cognia-04-base-de-datos"] = dashboard("cognia-bd", "COGNIA 4 - Base de datos", p, ["operate"])

# 5. Negocio
p = [
    stat("Pedidos creados (1h)", 'sum(increase(cognia_pedidos_creados_total[1h]))', 0, 0, w=6, decimals=0),
    stat("Ventas confirmadas (1h)", 'sum(increase(cognia_ventas_confirmadas_total[1h]))', 6, 0, w=6, decimals=0),
    stat("Monto vendido (1h, COP)", 'sum(increase(cognia_ventas_monto_total[1h]))', 12, 0, w=6, unit="currencyUSD", decimals=0),
    stat("Pedidos compensados (1h)", 'sum(increase(cognia_pedidos_compensados_total[1h])) or vector(0)', 18, 0, w=6, decimals=0, thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 1}]),
    ts("Pedidos creados por metodo de entrega", [('sum by (metodo) (increase(cognia_pedidos_creados_total[5m]))', "{{metodo}}")], 0, 4),
    ts("Pagos simulados por resultado", [('sum by (estado) (increase(cognia_pagos_total[5m]))', "{{estado}}")], 12, 4),
    ts("Ventas confirmadas acumuladas", [('sum(cognia_ventas_confirmadas_total)', "ventas")], 0, 12),
    ts("Reservas de inventario", [('sum by (resultado) (increase(cognia_inventario_reservas_total[5m]))', "{{resultado}}")], 12, 12),
    ts("Pedidos compensados por motivo", [('sum by (motivo) (increase(cognia_pedidos_compensados_total[5m]))', "{{motivo}}")], 0, 20),
    ts("Registros de usuarios", [('sum(increase(cognia_auth_registros_total[5m]))', "registros")], 12, 20),
]
dashboards["cognia-05-negocio"] = dashboard("cognia-negocio", "COGNIA 5 - Operaciones de negocio", p, ["negocio"])

# 6. Seguridad
p = [
    stat("Autenticaciones fallidas (5 min)", 'sum(increase(cognia_auth_failures_total[5m]))', 0, 0, w=6, decimals=0, thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 5}, {"color": "red", "value": 10}]),
    stat("Accesos denegados 403 (5 min)", 'sum(increase(cognia_security_denied_total[5m]))', 6, 0, w=6, decimals=0, thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 5}]),
    stat("No autorizados 401 (5 min)", 'sum(increase(cognia_security_unauthorized_total[5m]))', 12, 0, w=6, decimals=0, thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 10}]),
    stat("Bloqueos por limite de intentos (1h)", 'sum(increase(cognia_auth_failures_total{motivo="limite_intentos"}[1h]))', 18, 0, w=6, decimals=0),
    ts("Autenticaciones fallidas por motivo", [('sum by (motivo) (increase(cognia_auth_failures_total[5m]))', "{{motivo}}")], 0, 4),
    ts("Inicios de sesion exitosos", [('sum(increase(cognia_auth_logins_total[5m]))', "exitosos")], 12, 4),
    ts("401 y 403 por servicio", [('sum by (application) (increase(cognia_security_unauthorized_total[5m]))', "401 {{application}}"), ('sum by (application) (increase(cognia_security_denied_total[5m]))', "403 {{application}}")], 0, 12),
    ts("Respuestas 401/403/429 (HTTP)", [(f'sum by (status) (rate({REQ}_count{{status=~"401|403|429"}}[1m]))', "{{status}}")], 12, 12, unit="reqps"),
]
dashboards["cognia-06-seguridad"] = dashboard("cognia-seguridad", "COGNIA 6 - Eventos de seguridad (agregados)", p, ["seguridad"])

OUT.mkdir(exist_ok=True)
for name, d in dashboards.items():
    (OUT / f"{name}.json").write_text(json.dumps(d, indent=2, ensure_ascii=False), encoding="utf-8")
print(f"{len(dashboards)} paneles generados en {OUT}")
