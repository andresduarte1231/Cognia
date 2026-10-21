# Seguridad: controles y verificación

| Control | Implementación | Verificación |
|---|---|---|
| Contraseñas | BCrypt | prueba unitaria `SeguridadAuthTest` |
| JWT | HS256, 60 min, rechazo de firma alterada, `alg=none` y expirados | `SeguridadAuthTest` (7) |
| Fuerza bruta | 5 fallos / 15 min por cuenta (en memoria) + alerta | unitaria + métricas |
| Autorización por rol | Spring Security en cada servicio | `api-smoke.ps1` (34/34 vía gateway) |
| Idempotencia de pedidos | cabecera `Idempotency-Key` | `PedidoServiceTest`, smoke |
| Integridad de inventario | bloqueo pesimista, transacciones | `InventarioServiceTest` |
| Cabeceras | CSP y demás en el gateway | revisión manual de respuestas |
| Secretos | `.env` ignorado por Git; usuario `cognia_app` de mínimo privilegio | `git ls-files` sin secretos |
| Métricas | Actuator con autenticación básica; solo `/actuator/health` público | 401 sin credenciales en los 4 servicios y Prometheus |
| Observabilidad | Prometheus/Grafana solo en 127.0.0.1, con login | comprobado |
| Datos personales | Política y términos (Ley 1581 de 2012) en el frontend | revisión manual |

## Limitaciones conocidas
- Sin revocación de JWT en servidor (el cierre de sesión solo se audita).
- El limitador de intentos es en memoria (se pierde al reiniciar).
- Las pruebas unitarias no cubren roles HTTP; los cubre el smoke test.
- No se realizó un escaneo OWASP ZAP ni pruebas de carga.
- Cambiar la clave del propietario de desarrollo y revisar el usuario antiguo `app_cognia` de MySQL (privilegios amplios, no usado por la aplicación).