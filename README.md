# DisGas Backend — Gas El Volcán

API REST con **Spring Boot 4 (Java 21)**, **MySQL**, **Flyway**, **Spring Security + JWT** y **Swagger (springdoc)**.
Backend de la tienda de gas a domicilio (frontend: `FS2_DisGas`).

## Ejecutar en local (sin instalar nada más)

```bash
./mvnw spring-boot:run
```

Usa una base **H2 en memoria** (modo MySQL) y carga usuarios de demostración. Luego abre:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Salud: http://localhost:8080/actuator/health

Para probar endpoints protegidos: `POST /v1/auth/login` → copia `accessToken` → botón **Authorize** en Swagger.

### Cuentas de demostración (solo perfil de desarrollo)

| Rol | Correo | Contraseña |
|---|---|---|
| ADMINISTRADOR | admin@volcan.cl | admin123 |
| VENDEDOR | ana.vendedora@volcan.cl | 123456 |
| REPARTIDOR | carlos.repartidor@volcan.cl | 123456 |
| CLIENTE | juan.perez@email.com | 123456 |

## Modelo de datos (Anexo 1)

`USUARIO → BOLETA → DETALLE_BOLETA ← PRODUCTO ← CATEGORIA`. El esquema vive en `src/main/resources/db/migration` (Flyway):
`V1` crea las tablas y `V2` carga categorías y el catálogo inicial.

## Roles y permisos

| Recurso | ADMINISTRADOR | VENDEDOR | REPARTIDOR | CLIENTE | Público |
|---|:-:|:-:|:-:|:-:|:-:|
| Registro / login / refresh | ✔ | ✔ | ✔ | ✔ | ✔ |
| Ver productos y categorías | ✔ (incluye inactivos) | ✔ (incluye inactivos) | ✔ | ✔ | ✔ |
| Crear/editar/eliminar productos, categorías, stock | ✔ | – | – | – | – |
| Ver boletas (lista y detalle) | todas | todas | las asignadas | las propias | – |
| Crear pedido | ✔ | – | – | ✔ | – |
| Asignar repartidor | ✔ | – | – | – | – |
| Cambiar estado de boleta | todas las transiciones válidas | – | EN_RUTA / ENTREGADO (las suyas) | solo cancelar las suyas pendientes | – |
| Registrar pago en efectivo | ✔ | – | las asignadas | – | – |
| CRUD de usuarios | ✔ | – | – | – | – |
| Mi perfil / cambiar contraseña | ✔ | ✔ | ✔ | ✔ | – |

## Endpoints (todos bajo `/v1`)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/v1/auth/registro` · `/v1/auth/login` · `/v1/auth/refresh` | público |
| GET | `/v1/productos` · `/v1/productos/{id}` · `/v1/categorias` · `/v1/categorias/{id}` | público |
| POST/PUT/DELETE | `/v1/productos`, `/v1/productos/{id}` · `/v1/categorias`, `/v1/categorias/{id}` | ADMINISTRADOR |
| PATCH | `/v1/productos/{id}/stock` | ADMINISTRADOR |
| GET/PUT | `/v1/usuarios/me` · PUT `/v1/usuarios/me/password` | autenticado |
| GET/POST/PUT/PATCH/DELETE | `/v1/usuarios`, `/v1/usuarios/{id}`, `/v1/usuarios/{id}/estado` | ADMINISTRADOR |
| POST/GET | `/v1/boletas` · `/v1/boletas/{id}` | según rol |
| PATCH | `/v1/boletas/{id}/repartidor` | ADMINISTRADOR |
| PATCH | `/v1/boletas/{id}/estado` | ADMINISTRADOR, REPARTIDOR, CLIENTE |
| POST | `/v1/boletas/{id}/pago-efectivo` | ADMINISTRADOR, REPARTIDOR |

Códigos: `200/201/204` éxito · `400` datos inválidos · `401` sin token / token inválido · `403` sin permiso · `404` no existe · `409` conflicto (correo duplicado, stock insuficiente, transición inválida). Todos los errores usan el mismo formato JSON (`timestamp, status, error, mensaje, ruta, detalles`).

## Reglas de negocio principales

- El **precio y el total los calcula el servidor**; el cliente solo envía producto y cantidad.
- Al crear una boleta se **valida y descuenta el stock** (con bloqueo de fila para evitar sobreventa); al cancelarla se **repone**.
- Flujo de estado: `PENDIENTE → EN_RUTA → ENTREGADO`, o `CANCELADO` (no si ya está pagada). Para pasar a `EN_RUTA` debe haber repartidor asignado.
- El registro público siempre crea `CLIENTE`. Un admin no puede borrarse, desactivarse ni quitarse el rol a sí mismo.
- No se puede eliminar un producto/usuario/categoría con datos asociados (409); se desactiva en su lugar.

## Seguridad (JWT)

- `POST /v1/auth/login` entrega **access token** (30 min, lleva el rol) y **refresh token** (7 días).
- `POST /v1/auth/refresh` valida el refresh token, confirma que el usuario siga activo y emite un par nuevo.
- Firma HS256 con `JWT_SECRET` (≥ 32 caracteres). El claim `token_type` impide usar un refresh token como access token.
- Contraseñas con **BCrypt**; la API nunca devuelve contraseñas ni hashes.
- Sin sesión en servidor (stateless), CSRF desactivado (no hay cookies), CORS restringido a `CORS_ALLOWED_ORIGINS`.
- Autorización en dos capas: reglas por URL (`SecurityConfig`) y `@PreAuthorize` en cada controlador.

## Pruebas

```bash
./mvnw test
```

- `BoletaServiceTest`: lógica de negocio (total, stock, estados, permisos) con Mockito.
- `ApiSeguridadIntegrationTest`: API completa con JWT y roles sobre H2 + Flyway.

## Variables de entorno (producción / AWS)

Perfil `prod` (`SPRING_PROFILES_ACTIVE=prod`). Ver `.env.example`.

| Variable | Descripción |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión MySQL (RDS) |
| `JWT_SECRET` | Clave de firma JWT (≥ 32 caracteres). **Obligatoria** |
| `CORS_ALLOWED_ORIGINS` | URL(s) del frontend, separadas por coma. **Obligatoria** |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Crea el primer administrador si no existe ninguno |
| `PORT` | Puerto (por defecto 8080) |

En `prod` **no** se cargan los usuarios de demostración.

### Probar el contenedor como en producción

```bash
cp .env.example .env      # editar valores
docker compose up --build
```

### Notas para el despliegue en AWS

- Base de datos: **RDS MySQL 8** (crear la BD `disgas` con `utf8mb4`). Flyway crea las tablas al primer arranque.
- Imagen: el `Dockerfile` genera una imagen lista para ECR → ECS/Fargate, App Runner o una EC2 con Docker.
- Health check del balanceador: `GET /actuator/health`.
- Las variables sensibles van en Secrets Manager / Parameter Store, no en el repositorio.
- El security group de RDS debe permitir el puerto 3306 solo desde el backend.
