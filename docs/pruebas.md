# Pruebas y resultados

| Suite | Comando | Resultado |
|---|---|---|
| Backend unitarias (orders 14, catalog 9, auth 7) | `mvn test` | 30/30 pasan |
| Frontend (Vitest) | `npm test` | 7/7 pasan |
| API integración vía gateway | `tests\api-smoke.ps1` | 34/34 pasan (repetido con Prometheus activo) |
| Caída y recuperación | `tests\operate-outage.ps1` | alerta en 35 s, recuperación en 30 s |
| Render SPA (Edge headless) | manual | landing con 8 productos reales |

No ejecutado: pruebas E2E autenticadas en navegador del flujo de compra, pruebas de carga y escaneo de vulnerabilidades.