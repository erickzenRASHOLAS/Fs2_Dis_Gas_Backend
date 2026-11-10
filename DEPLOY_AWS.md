# Despliegue en AWS (EC2 + RDS + S3), sin Docker

Sigue el orden del taller DSY1104: grupos de seguridad → RDS → EC2 → S3. Región **us-east-1**, VPC por defecto.

```
Navegador ──(1)──► S3 (frontend)
    └──────(2)──► EC2 (backend, :8080) ──(3)──► RDS (MySQL, privado)
```

## 0. Compilar el JAR (en tu computador)

```bash
./mvnw -DskipTests package          # Windows: mvnw.cmd -DskipTests package
# genera: target/DisGasBackend-0.0.1-SNAPSHOT.jar
```

## 1. Grupos de seguridad

| Grupo | Entrada | Origen |
|---|---|---|
| `sg-backend` | TCP 22 (SSH) | Mi IP |
| `sg-backend` | TCP 8080 (API) | Cualquier origen |
| `sg-rds` | TCP 3306 (MySQL) | **El grupo `sg-backend`** (no una IP) |

## 2. RDS (base de datos)

- Motor **MySQL**, **Single-AZ**, la clase más pequeña disponible, grupo de seguridad `sg-rds`.
- **Public access: No.**
- En *Additional configuration* → **Initial database name: `disgas`** (la app necesita que la base exista; las tablas las crea ella sola).
- Anota el **endpoint** (será `DB_HOST`), el usuario y la contraseña maestra. Tarda ~10 min: sigue con EC2 mientras tanto.

## 3. EC2 (backend)

1. Lanzar **Amazon Linux 2023** con `sg-backend` y un par de claves `.pem`.
2. Instalar **Java 17 (Amazon Corretto)**:

```bash
ssh -i clave.pem ec2-user@<IP-PUBLICA>
sudo dnf install -y java-17-amazon-corretto-headless
java -version                      # debe mostrar Corretto-17
```

3. Subir el JAR (desde tu computador):

```bash
scp -i clave.pem target/DisGasBackend-0.0.1-SNAPSHOT.jar ec2-user@<IP-PUBLICA>:/home/ec2-user/app.jar
```

4. Guardar las variables en `/etc/app.env` (plantilla en `deploy/app.env.example`):

```bash
openssl rand -hex 32               # genera el valor para JWT_SECRET
sudo nano /etc/app.env             # pegar la plantilla y reemplazar los valores
sudo chmod 600 /etc/app.env
```

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_HOST` | endpoint de RDS |
| `DB_NAME` | `disgas` |
| `DB_USER` / `DB_PASSWORD` | credenciales de RDS |
| `JWT_SECRET` | clave de 32+ caracteres |
| `CORS_ORIGIN` | URL del frontend en S3, **sin barra final** |

5. Crear el servicio (contenido en `deploy/app.service`) y activarlo:

```bash
sudo nano /etc/systemd/system/app.service
sudo systemctl daemon-reload
sudo systemctl enable --now app
sudo systemctl status app
sudo journalctl -u app -f          # ver el arranque; busca "Started DisGasBackendApplication"
```

Si la instancia se reinicia, el servicio arranca solo.

## 4. Probar el backend

```bash
curl http://<IP-PUBLICA>:8080/v1/productos            # devuelve el catálogo en JSON
curl -X POST http://<IP-PUBLICA>:8080/v1/auth/login -H "Content-Type: application/json" \
     -d '{"correo":"admin@volcan.cl","password":"admin123"}'
```

Swagger: `http://<IP-PUBLICA>:8080/swagger-ui.html`

## 5. S3 (frontend)

Requiere que el frontend ya use la API. Pasos del taller:

1. Compilar con la URL del backend: `VITE_API_URL=http://<IP>:8080 npm run build`
2. Crear el bucket y **quitar el bloqueo de acceso público**.
3. Activar **Static website hosting** (index document `index.html`).
4. Política de lectura pública:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow", "Principal": "*", "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::NOMBRE-DEL-BUCKET/*"
  }]
}
```

5. Subir el contenido de `dist/`.
6. Poner la URL del bucket en `CORS_ORIGIN` (en `/etc/app.env`) y reiniciar: `sudo systemctl restart app`.

## 6. Errores frecuentes

| Síntoma | Qué revisar |
|---|---|
| `:8080` no responde | Regla 8080 en `sg-backend` y `sudo systemctl status app` |
| `Communications link failure` | `sg-rds` acepta a `sg-backend`; `DB_HOST` correcto |
| `Unknown database 'disgas'` | RDS creada sin *Initial database name* (créala a mano o recrea la RDS) |
| `Access denied for user` | `DB_USER` / `DB_PASSWORD` |
| La app no arranca y menciona una variable | Falta una variable en `/etc/app.env` (`JWT_SECRET`, `DB_HOST`...) |
| `Blocked by CORS policy` | `CORS_ORIGIN` idéntico al origen del navegador, sin barra final; reiniciar el servicio |
| S3 responde 403 | Bloqueo de acceso público y política del bucket |
| Nada responde al reabrir el laboratorio | Cambió la IP de EC2: recompilar el frontend con la IP nueva |

Primer reflejo siempre: `sudo journalctl -u app -n 100`.

## 7. Checklist de evidencias (antes de limpiar)

- [ ] URL del frontend en S3
- [ ] Endpoint GET del backend con JSON (`/v1/productos`)
- [ ] Reglas de `sg-backend` y `sg-rds`
- [ ] RDS con *Public access: No*
- [ ] `systemctl status app` en ejecución
- [ ] Consola del navegador sin errores de CORS
- [ ] Repositorio sin secretos (sin contraseñas ni claves `.pem`)
- [ ] Recursos eliminados o detenidos

## 8. Limpieza

1. Vaciar y eliminar el bucket · 2. **Terminar** la instancia EC2 · 3. Eliminar la base en RDS ·
4. Eliminar `sg-rds` y luego `sg-backend` · 5. Eliminar el par de claves · 6. Revisar el gasto al día siguiente.

RDS sigue cobrando con el laboratorio cerrado. Si la evaluación necesita la app viva: detener EC2 y RDS (RDS se reenciende sola a los 7 días).

## 9. Preguntas de cierre del taller (respuestas cortas)

1. **¿Por qué la base no tiene acceso público y cómo llega el backend?** Para que nadie desde internet pueda intentar conectarse. `sg-rds` solo acepta el puerto 3306 desde el grupo `sg-backend`, así que solo la instancia EC2 llega a la base.
2. **¿Qué efecto tiene una barra final en `CORS_ORIGIN`?** El navegador envía el origen sin barra (`http://sitio`), y CORS compara texto exacto; con barra final no coincide y el navegador bloquea las llamadas.
3. **¿Qué cambiar para servir el frontend por HTTPS?** Poner CloudFront delante de S3 (con certificado) y también HTTPS en el backend (balanceador en el puerto 443); si no, el navegador bloquea las llamadas por contenido mixto. Además `CORS_ORIGIN` pasa a ser `https://...`.

## Atajos del taller (no llevar a producción real)

Secretos en `/etc/app.env` (en producción: Secrets Manager / Parameter Store), HTTP sin cifrar, puerto 8080 abierto, `ddl-auto=update` (en producción: migraciones con Flyway) y usuarios de demostración (poner `DEMO_USERS=false`).
