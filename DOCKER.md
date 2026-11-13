# Despliegue con Docker (objetivo principal)

Con un solo comando se levantan **dos contenedores**: la base de datos MySQL y el backend Spring Boot.
Es el mismo esquema del taller del profesor, que replica AWS en tu computador:

```
Tu navegador / Swagger / Postman
        │  http://localhost:8080
        ▼
┌───────────────────┐   jdbc:mysql://db:3306   ┌───────────────────┐
│ contenedor backend│ ───────────────────────► │ contenedor db     │
│ Spring Boot (Java)│                          │ MySQL 8.4         │
└───────────────────┘                          └─────────┬─────────┘
   equivale a EC2                                        │ volumen db_data (los datos se guardan aquí)
                                                         ▼          equivale a RDS
```

## 1. Requisitos

- **Docker Desktop** instalado y **abierto** (ícono de la ballena sin errores). En Windows usa WSL 2.
- Comprobar en una terminal (PowerShell):

```powershell
docker --version
docker compose version
```

## 2. Paso a paso

1. Abre una terminal **en la carpeta del backend** (donde está `docker-compose.yml`). En IntelliJ: pestaña *Terminal*.
2. *(Opcional)* Para cambiar claves: `copy .env.example .env` y edita `.env`. Si no lo haces, se usan valores por defecto que ya funcionan.
3. Levanta todo:

```powershell
docker compose up --build
```

   La primera vez demora unos minutos (descarga Maven, MySQL y compila). Está listo cuando el backend muestra
   `Started DisGasBackendApplication`.

4. Verifica:

| Qué | Dirección |
|---|---|
| Swagger (documentación y pruebas) | http://localhost:8080/swagger-ui.html |
| Catálogo (JSON) | http://localhost:8080/v1/productos |
| Salud de la app y la base de datos | http://localhost:8080/actuator/health → `{"status":"UP"}` |

```powershell
Invoke-RestMethod http://localhost:8080/v1/productos
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod -Method Post -Uri http://localhost:8080/v1/auth/login -ContentType "application/json" -Body '{"correo":"admin@volcan.cl","password":"admin123"}'
```

5. Prueba el flujo completo en Swagger: `POST /v1/auth/login` → copia el `token` → botón **Authorize** → prueba `GET /v1/usuarios` (solo ADMINISTRADOR).

## 3. Comandos del día a día

| Comando | Para qué |
|---|---|
| `docker compose up --build -d` | Levantar en segundo plano (con `--build` se recompila si cambiaste código) |
| `docker compose ps` | Ver qué contenedores están corriendo |
| `docker compose logs -f backend` | Ver el log del backend en vivo (Ctrl+C para salir) |
| `docker compose logs db` | Log de MySQL |
| `docker compose restart backend` | Reiniciar solo el backend |
| `docker compose down` | Apagar todo (**los datos se conservan**) |
| `docker compose down -v` | Apagar y **borrar la base de datos** (empieza desde cero) |

## 4. Cómo leer los archivos Docker

**`Dockerfile`** — receta para fabricar la imagen del backend, en 2 etapas:

| Línea | Qué hace |
|---|---|
| `FROM maven:3.9-eclipse-temurin-17 AS build` | Etapa 1: una imagen con Maven y Java 17 para **compilar** |
| `COPY pom.xml .` + `RUN mvn ... dependency:go-offline` | Descarga las librerías primero; así Docker las guarda en caché y los siguientes builds son rápidos |
| `COPY src ./src` + `RUN mvn ... package -Dmaven.test.skip=true` | Copia el código y genera `target/app.jar` (sin correr las pruebas dentro de la imagen) |
| `FROM eclipse-temurin:17-jre` | Etapa 2: una imagen liviana solo con Java para **ejecutar** |
| `COPY --from=build ... app.jar` | Trae únicamente el JAR (la imagen final no lleva Maven ni el código fuente) |
| `USER spring` | La app no corre como administrador dentro del contenedor |
| `ENTRYPOINT ["java","-Xmx512m","-jar","app.jar"]` | Comando que se ejecuta al iniciar; `-Xmx512m` limita la memoria |

**`docker-compose.yml`** — describe cómo se levantan y conectan los contenedores:

| Parte | Qué hace |
|---|---|
| `services: db` | Contenedor MySQL. Crea la base `disgas` y el usuario con las variables `MYSQL_*` |
| `volumes: db_data` | Guarda los datos fuera del contenedor para que no se pierdan |
| `healthcheck` | Pregunta a MySQL si ya responde |
| `services: backend` → `build: .` | Construye la imagen con el `Dockerfile` |
| `depends_on ... service_healthy` | El backend **espera** a que MySQL esté listo |
| `environment:` | Variables de entorno del backend: perfil `prod`, datos de la base, clave JWT y CORS |
| `DB_HOST: db` | Dentro de Docker, los contenedores se encuentran por el **nombre del servicio** (no `localhost`) |
| `ports: "8080:8080"` | Publica el puerto del contenedor en tu computador (`computador:contenedor`) |
| `${DB_USER:-disgas}` | "usa la variable `DB_USER` del `.env`; si no existe, usa `disgas`" |

**`application-prod.properties`** — lee esas variables (`${DB_HOST}`, `${JWT_SECRET}`...) y conecta a MySQL. Es el mismo archivo que se usa en AWS.

## 5. Docker ↔ AWS

| En Docker | En AWS |
|---|---|
| contenedor `db` | RDS (MySQL) |
| contenedor `backend` | instancia EC2 |
| `environment:` del compose | archivo `/etc/app.env` |
| `CORS_ORIGIN` | URL del frontend en S3 |

Para llevar el backend a AWS con Docker, ver **`DEPLOY_AWS.md`** (opción A).

## 6. Errores frecuentes

| Síntoma | Causa y solución |
|---|---|
| `error during connect` / `Cannot connect to the Docker daemon` | Docker Desktop no está abierto. Ábrelo y espera a que diga *Running* |
| `port is already allocated` (8080) | Otro programa usa el 8080 (por ejemplo la app corriendo en IntelliJ). Deténlo o cambia a `"8081:8080"` en el compose |
| `port is already allocated` (3307) | Cambia a `"127.0.0.1:3308:3306"` o quita esa línea (es opcional) |
| `Communications link failure` en el backend | MySQL aún no estaba listo. Se reintenta solo; si persiste: `docker compose logs db` |
| `Access denied for user` | Cambiaste `DB_PASSWORD` después de crear la base: MySQL conserva la clave antigua. Solución: `docker compose down -v` y volver a levantar |
| `Could not resolve placeholder 'JWT_SECRET'` | Falta la variable (solo pasa si ejecutas la imagen con `docker run` sin `-e`/`--env-file`) |
| `WeakKeyException` | `JWT_SECRET` tiene menos de 32 caracteres |
| Falla el build en `mvn package` | Lee el error que aparece justo arriba; reintenta con `docker compose build --no-cache` |
| Cambié código y no se nota | Falta recompilar: `docker compose up --build` |
| `Blocked by CORS policy` desde el frontend | `CORS_ORIGIN` debe ser idéntico al origen del navegador, **sin barra final**; luego `docker compose up -d` |

## 7. Evidencias para la presentación

- [ ] `docker compose ps` con `disgas-db` y `disgas-backend` en estado *running*
- [ ] Swagger abierto en http://localhost:8080/swagger-ui.html
- [ ] `/actuator/health` con `"status":"UP"`
- [ ] Log del backend con `The following 1 profile is active: "prod"` (demuestra que usa MySQL)
- [ ] Tablas en MySQL: `docker compose exec db mysql -u disgas -pdisgas1234 disgas -e "SHOW TABLES;"`
- [ ] Login y una ruta protegida probados con distintos roles
- [ ] Repositorio sin secretos (`.env` no se sube)

Más sobre la base de datos (diagrama, respaldo, cómo revisarla): `docs/BASE_DE_DATOS.md`.
