# DisGas Backend — Gas El Volcán

API REST con **Spring Boot**, **MySQL**, **Spring Security + JWT** y **Swagger**, lista para desplegar con **Docker**.
Backend de la tienda de gas a domicilio (frontend: `FS2_DisGas`).

## Inicio rápido con Docker (recomendado)

Requiere Docker Desktop abierto. En la carpeta del proyecto:

```powershell
docker compose up --build
```

- Swagger: http://localhost:8080/swagger-ui.html
- Catálogo: http://localhost:8080/v1/productos
- Salud: http://localhost:8080/actuator/health

Guía completa, comandos y errores frecuentes: **[DOCKER.md](DOCKER.md)**.

## Ejecutar sin Docker (desarrollo)

No necesitas base de datos: usa **H2 en memoria**.

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run   (o el botón Run de IntelliJ)
```

**Probar rutas protegidas en Swagger:** `POST /v1/auth/login` → copiar el campo `token` → botón **Authorize** → pegar el token.

### Usuarios de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| ADMINISTRADOR | admin@volcan.cl | admin123 |
| VENDEDOR | ana.vendedora@volcan.cl | 123456 |
| CLIENTE | juan.perez@email.com | 123456 |

## Documentación

| Archivo | Contenido |
|---|---|
| [DOCKER.md](DOCKER.md) | Despliegue con Docker paso a paso |
| [docs/BASE_DE_DATOS.md](docs/BASE_DE_DATOS.md) | Modelo de datos con diagrama, gestión y respaldo de la base |
| [DEPLOY_AWS.md](DEPLOY_AWS.md) | Llevar el backend a AWS (EC2 + RDS) |
| [GUIA_DE_ESTUDIO.md](GUIA_DE_ESTUDIO.md) | Qué explicar en la presentación, indicador por indicador |

## Estructura del proyecto

```
src/main/java/cl/duoc/DisGasBackend
├── model/        Entidades = tablas (Usuario, Categoria, Producto, Boleta, DetalleBoleta) y enums
├── repository/   Acceso a la base de datos (Spring Data JPA)
├── service/      LÓGICA DE NEGOCIO
├── controller/   Endpoints REST (/v1/...)
├── dto/          Datos que llegan en el JSON de las peticiones (con validaciones)
├── security/     JwtUtil (crea/lee tokens) y JwtAuthFilter (revisa el token en cada petición)
├── config/       SecurityConfig (roles por ruta), CorsConfig, OpenApiConfig (Swagger), DataInitializer (datos iniciales)
└── exception/    ManejadorErrores (respuestas de error en JSON)
Dockerfile · docker-compose.yml · .env.example      Despliegue con Docker
docs/                                               Documentación y diagrama de la base de datos
```

## Modelo de datos

`USUARIO → BOLETA → DETALLE_BOLETA ← PRODUCTO ← CATEGORIA` (Figura 1 de la pauta). Diagrama y explicación en [docs/BASE_DE_DATOS.md](docs/BASE_DE_DATOS.md).
Hibernate crea las tablas solo (`spring.jpa.hibernate.ddl-auto=update`) y `DataInitializer` carga el catálogo y los usuarios de demo.

## Roles y permisos (pauta)

| Acción | ADMINISTRADOR | VENDEDOR | CLIENTE | Sin sesión |
|---|:-:|:-:|:-:|:-:|
| Registro, login, refresh | ✔ | ✔ | ✔ | ✔ |
| Ver catálogo (`GET /v1/productos`, `/v1/categorias`) | ✔ | ✔ | ✔ | ✔ |
| Ver todos los productos, incluso inactivos (`/v1/productos/todos`) | ✔ | ✔ | – | – |
| Crear / editar / eliminar productos y categorías | ✔ | – | – | – |
| Ver lista y detalle de boletas | todas | todas | solo las suyas | – |
| Crear boleta (comprar) | ✔ | – | ✔ | – |
| Cancelar boleta | cualquiera no entregada | – | sus boletas pendientes | – |
| Cambiar estado de boleta | ✔ | – | – | – |
| CRUD de usuarios | ✔ | – | – | – |
| Ver / editar mi perfil | ✔ | ✔ | ✔ | – |

## Endpoints (todos bajo `/v1`)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/v1/auth/registro` · `/v1/auth/login` · `/v1/auth/refresh` | público |
| GET | `/v1/productos` · `/v1/productos/{id}` · `/v1/categorias` · `/v1/categorias/{id}` | público |
| GET | `/v1/productos/todos` | ADMINISTRADOR, VENDEDOR |
| POST · PUT · DELETE | `/v1/productos[/{id}]` · `/v1/categorias[/{id}]` | ADMINISTRADOR |
| GET · POST · PUT · DELETE | `/v1/usuarios[/{id}]` | ADMINISTRADOR |
| GET · PUT | `/v1/perfil` | cualquier usuario con sesión |
| POST | `/v1/boletas` | CLIENTE, ADMINISTRADOR |
| GET | `/v1/boletas` · `/v1/boletas/{id}` | ADMINISTRADOR, VENDEDOR, CLIENTE |
| PUT | `/v1/boletas/{id}/cancelar` | CLIENTE, ADMINISTRADOR |
| PUT | `/v1/boletas/{id}/estado` | ADMINISTRADOR |
| GET | `/actuator/health` | público |

Códigos: `200/201/204` éxito · `400` datos inválidos · `401` sin token / token inválido · `403` sin permiso · `404` no existe · `409` conflicto (correo repetido, sin stock, cambio de estado inválido).

## Reglas de negocio

1. El **total lo calcula el servidor** con los precios de la base de datos (el cliente solo envía producto y cantidad).
2. Al comprar se **valida y descuenta el stock**; si falta stock responde `409` y no se descuenta nada (`@Transactional`).
3. Al cancelar una boleta se **devuelve el stock**.
4. El estado avanza `PENDIENTE → EN_RUTA → ENTREGADO`; cualquier otro salto da `409`.
5. Un cliente solo ve y cancela **sus** boletas.
6. El registro público **siempre crea CLIENTE**.
7. No se borra un producto que ya está en boletas, ni una categoría con productos, ni un usuario con boletas (`409`).

## Seguridad

- **Login** devuelve `token` (60 min) y `refreshToken` (7 días), firmados con la clave `jwt.secret`.
- **Renovación:** `POST /v1/auth/refresh` con el `refreshToken` entrega un par nuevo.
- `JwtAuthFilter` valida el token en cada petición; `SecurityConfig` decide por rol qué rutas puede usar.
- Contraseñas con **BCrypt**. La API nunca devuelve contraseñas.
- Sin sesión en el servidor (*stateless*). CORS limitado al origen del frontend.

## Pruebas

```bash
./mvnw test
```

- `BoletaServiceTest`: lógica de negocio (unitaria, con Mockito).
- `ApiIntegrationTest`: API completa con JWT y roles sobre H2.

## Variables de entorno (perfil `prod`: Docker y AWS)

| Variable | Descripción | En `docker-compose.yml` |
|---|---|---|
| `DB_HOST` | Host de MySQL | `db` (nombre del servicio) |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Base y credenciales | valores por defecto o `.env` |
| `JWT_SECRET` | Clave de firma (32+ caracteres) | valor por defecto local |
| `CORS_ORIGIN` | URL del frontend, sin barra final | `http://localhost:3000,http://localhost:5173` |
| `DEMO_USERS` | `false` desactiva los usuarios de demo | `true` |
