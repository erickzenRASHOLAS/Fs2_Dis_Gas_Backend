# Despliegue en AWS (EC2 + RDS + S3)

Orden del taller DSY1104: grupos de seguridad → RDS → EC2 → S3. Región **us-east-1**.
Antes de AWS, comprueba que todo funciona en tu computador con Docker (**`DOCKER.md`**): si funciona allí, en AWS cambian solo las variables.

```
Navegador ──► S3 (frontend)
    └──────► EC2 (backend, :8080) ──► RDS (MySQL, privado)
```

## 1. Grupos de seguridad

| Grupo | Entrada | Origen |
|---|---|---|
| `sg-backend` | TCP 22 (SSH) | Mi IP |
| `sg-backend` | TCP 8080 (API) | Cualquier origen |
| `sg-rds` | TCP 3306 (MySQL) | **El grupo `sg-backend`** (no una IP) |

## 2. RDS (base de datos)

- Motor **MySQL**, **Single-AZ**, clase más pequeña, grupo `sg-rds`, **Public access: No**.
- En *Additional configuration* → **Initial database name: `disgas`** (las tablas las crea la app sola, pero la base debe existir).
- Anota el **endpoint** (será `DB_HOST`), usuario y contraseña. Tarda ~10 min.

## 3. EC2 y variables de entorno

Lanza **Amazon Linux 2023** con `sg-backend` y un par de claves `.pem`. Conéctate y crea las variables
(plantilla en `deploy/app.env.example`; `openssl rand -hex 32` genera el `JWT_SECRET`):

```bash
ssh -i clave.pem ec2-user@<IP-PUBLICA>
sudo nano /etc/app.env          # pegar la plantilla y reemplazar valores
sudo chmod 600 /etc/app.env
```

Elige **una** de las dos formas de ejecutar el backend:

### Opción A — Con Docker (coincide con el objetivo del proyecto)

1. **En tu computador**, construye la imagen y expórtala (el `Dockerfile` ya existe):

```powershell
docker build -t disgas-backend .
docker save disgas-backend -o disgas-backend.tar
scp -i clave.pem disgas-backend.tar ec2-user@<IP-PUBLICA>:/home/ec2-user/
```

   *(Si usas una Mac con chip Apple, construye con `docker build --platform linux/amd64 ...`.)*

2. **En la EC2**, instala Docker, carga la imagen y ejecútala:

```bash
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user     # luego cierra la sesión SSH y vuelve a entrar

docker load -i disgas-backend.tar
docker run -d --name disgas-backend --restart unless-stopped \
  --env-file /etc/app.env -p 8080:8080 disgas-backend
docker logs -f disgas-backend        # busca "Started DisGasBackendApplication"
```

   `--env-file` entrega las variables al contenedor y `--restart unless-stopped` lo reinicia si la instancia se reinicia.
   Para actualizar: construir y subir la imagen de nuevo, `docker rm -f disgas-backend` y repetir `docker run`.

### Opción B — JAR directo con Java (como el taller original)

```bash
./mvnw -DskipTests package           # genera target/app.jar
scp -i clave.pem target/app.jar ec2-user@<IP-PUBLICA>:/home/ec2-user/app.jar
# En la EC2:
sudo dnf install -y java-17-amazon-corretto-headless
sudo nano /etc/systemd/system/app.service     # contenido de deploy/app.service
sudo systemctl daemon-reload && sudo systemctl enable --now app
sudo journalctl -u app -f
```

## 4. Probar el backend

```bash
curl http://<IP-PUBLICA>:8080/actuator/health      # {"status":"UP"}
curl http://<IP-PUBLICA>:8080/v1/productos
```

Swagger: `http://<IP-PUBLICA>:8080/swagger-ui.html`

## 5. S3 (frontend)

Requiere que el frontend ya use la API. Compilar con `VITE_API_URL=http://<IP>:8080`, crear el bucket sin bloqueo de acceso público,
activar *Static website hosting*, política de lectura pública (`s3:GetObject`), subir `dist/` y poner la URL del bucket en `CORS_ORIGIN`
(sin barra final). Reiniciar el backend: `docker restart disgas-backend` (opción A) o `sudo systemctl restart app` (opción B).

## 6. Errores frecuentes

| Síntoma | Qué revisar |
|---|---|
| `:8080` no responde | Regla 8080 en `sg-backend`; `docker ps` / `sudo systemctl status app` |
| `Communications link failure` | `sg-rds` acepta a `sg-backend`; `DB_HOST` correcto |
| `Unknown database 'disgas'` | RDS creada sin *Initial database name* |
| `Access denied for user` | `DB_USER` / `DB_PASSWORD` en `/etc/app.env` |
| El contenedor se cae al iniciar | `docker logs disgas-backend`; casi siempre falta una variable (`JWT_SECRET`, `DB_HOST`...) |
| `permission denied` con `docker` | Falta `usermod -aG docker ec2-user` y volver a entrar por SSH |
| `Blocked by CORS policy` | `CORS_ORIGIN` idéntico al origen del navegador, sin barra final; reiniciar el backend |
| Nada responde al reabrir el laboratorio | Cambió la IP de EC2 |

## 7. Limpieza

Vaciar y eliminar el bucket · **terminar** la EC2 · eliminar la base en RDS · eliminar `sg-rds` y luego `sg-backend` · eliminar el par de claves · revisar el gasto al día siguiente (RDS sigue cobrando con el laboratorio cerrado).

## Atajos del laboratorio (no llevar a producción real)

Secretos en un archivo (en producción: Secrets Manager), HTTP sin cifrar, puerto 8080 abierto, `ddl-auto=update` (en producción: Flyway) y usuarios de demostración (`DEMO_USERS=false`).
