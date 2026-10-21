# COGNIA - Aquí es Toña

Tienda virtual de alimentos y productos de consumo. Arquitectura de microservicios con **Java 21 + Spring Boot (MVC)**, frontend **React + Vite**, **MariaDB (XAMPP)**, y observabilidad con **Prometheus + Grafana**. Sin Docker.

## Componentes y puertos
| Componente | Puerto | Tecnología |
|---|---|---|
| API Gateway **+ frontend React compilado** | 4000 | Spring Cloud Gateway (Boot 3.4.1, Cloud 2024.0.0) |
| auth-service (usuarios, JWT, roles) | 4001 | Spring Boot 3.3.5 |
| catalog-inventory-service | 4002 | Spring Boot 3.3.5 |
| orders-sales-service | 4003 | Spring Boot 3.3.5 |
| MariaDB 10.4 (XAMPP) | 3306 | solo base de datos |
| Prometheus / Grafana | 9090 / 3000 | solo 127.0.0.1 |

> Decisión de despliegue: el frontend ya **no** se publica con Tomcat/XAMPP; lo sirve el propio API Gateway (Spring Boot) desde `static/`. XAMPP se usa únicamente para MariaDB.

## Requisitos
Java 21, Maven 3.9, Node 20+ (solo para construir React), XAMPP (MariaDB), Python con `bcrypt` (solo para generar el hash de Prometheus), Git.

## Primera vez
1. Inicie MariaDB desde XAMPP (o deje que `start-all.ps1` lo haga).
2. Copie `.env.example` a `.env` y complete los valores (usuario `cognia_app` con privilegios solo sobre `cognia_aqui_es_tona`).
3. Migraciones y semillas: `.\database\scripts\apply.ps1`.
4. Compile los servicios: `mvn -q package` dentro de cada carpeta de `backend\*`.
5. Compile el frontend: `cd frontend; npm install; npm run build` (deja la salida en el gateway). Recompile después el gateway.

## Uso diario
```powershell
.\scripts\start-all.ps1            # MariaDB + 4 servicios Spring Boot
.\scripts\start-observability.ps1  # Prometheus + Grafana
# Aplicación:  http://localhost:4000
# Prometheus:  http://127.0.0.1:9090   (usuario/clave de métricas del .env)
# Grafana:     http://127.0.0.1:3000   (admin / GRAFANA_ADMIN_PASSWORD del .env)
.\scripts\stop-observability.ps1 ; .\scripts\stop-all.ps1
```
Desarrollo del frontend con recarga en caliente: `cd frontend; npm run dev` (puerto 5173, proxy a 4000).

## Pruebas
- Unitarias backend: `mvn test` en cada servicio (30 pruebas con JUnit 5/Mockito).
- Frontend: `cd frontend; npm test` (7 pruebas).
- Integración API (34 casos, vía gateway): `.\tests\api-smoke.ps1`.
- Disponibilidad/recuperación: `.\tests\operate-outage.ps1`.

## Documentación
- [Arquitectura](docs/arquitectura.md) · [Observabilidad (fase Operate)](docs/observabilidad.md) · [Seguridad](docs/seguridad.md) · [Pruebas y resultados](docs/pruebas.md)

## Credenciales de desarrollo
Propietario inicial: `propietario@cognia.local` / `Cognia2026Owner`. **Cámbiela antes de cualquier uso real.**