# Guía de estudio del backend (para la presentación)

Cada sección corresponde a un indicador de la rúbrica. Para cada uno: **qué decir** y **dónde está el código**.
Abre los archivos mientras lees; el código tiene comentarios en español.

---

## 1. Backend + base de datos + lógica de negocio (IE3.1.1 / IE3.1.2)

**Idea central:** la app tiene capas, cada una con una sola responsabilidad.

```
Petición HTTP → Controller → Service → Repository → Base de datos
                (recibe)    (reglas)   (consulta)    (tablas)
```

| Capa | Qué hace | Ejemplo |
|---|---|---|
| `controller/` | Recibe la petición y responde JSON. No tiene reglas. | `BoletaController` |
| `service/` | **Lógica de negocio**: valida y decide. | `BoletaService.crear` |
| `repository/` | Habla con la BD sin escribir SQL (Spring Data JPA). | `UsuarioRepository.findByCorreo` |
| `model/` | Cada `@Entity` es una tabla. | `Producto`, `Boleta` |

**Modelo de datos (5 tablas de la pauta):**
`Usuario 1—N Boleta`, `Boleta 1—N DetalleBoleta`, `Producto 1—N DetalleBoleta`, `Categoria 1—N Producto`.
Se ve en las anotaciones `@ManyToOne` / `@OneToMany` de `model/`.

**Conexión a la BD:** `application.properties` (H2 en desarrollo) y `application-prod.properties` (MySQL en RDS, con variables de entorno).
`spring.jpa.hibernate.ddl-auto=update` hace que Hibernate cree las tablas desde las entidades.

**Lógica de negocio** (`BoletaService`) — las 5 reglas que debes saber explicar:
1. El **total lo calcula el servidor** (no se confía en el cliente).
2. Al comprar se **valida y descuenta el stock**; sin stock → `409`.
3. Al cancelar se **devuelve el stock**.
4. El estado sigue un flujo: `PENDIENTE → EN_RUTA → ENTREGADO`.
5. Un cliente solo ve y cancela **sus** boletas.

`@Transactional` = "todo o nada": si falla el producto 3 de un pedido, se deshace el descuento de los productos 1 y 2.

---

## 2. API REST + Swagger (IE3.2.1 / IE3.2.3)

**Qué decir:** cada recurso tiene un controller con las 4 operaciones CRUD mapeadas a métodos HTTP:

| Operación | HTTP | Anotación | Ejemplo |
|---|---|---|---|
| Leer | GET | `@GetMapping` | `GET /v1/productos` |
| Crear | POST | `@PostMapping` | `POST /v1/productos` → 201 |
| Actualizar | PUT | `@PutMapping("/{id}")` | `PUT /v1/productos/3` |
| Eliminar | DELETE | `@DeleteMapping("/{id}")` | `DELETE /v1/productos/3` → 204 |

- **Versionado:** todas las rutas empiezan con `/v1` (`@RequestMapping("/v1/productos")`).
- **Validación:** `@Valid @RequestBody` + anotaciones en `Dtos.java` (`@NotBlank`, `@Email`, `@Min`). Si fallan → `400`.
- **Errores uniformes:** `ManejadorErrores` (`@RestControllerAdvice`). En los servicios lanzamos `ResponseStatusException(404/409...)` y ahí se convierte en JSON.
- **Swagger:** la dependencia `springdoc` genera la documentación sola. `OpenApiConfig` agrega el botón Authorize (JWT) y en los controllers `@Tag` y `@Operation` describen cada endpoint. URL: `/swagger-ui.html`.

**Demostración sugerida:** abrir Swagger → login → Authorize → crear un producto → listarlo → editarlo → eliminarlo.

---

## 3. Autenticación con roles y JWT (IE3.3.1 / IE3.3.4)

**Flujo completo (apréndelo en este orden):**

1. `POST /v1/auth/login` con correo y contraseña → `AuthService.login` busca al usuario y compara la clave con **BCrypt** (`passwordEncoder.matches`).
2. Si es correcta, `JwtUtil` crea 2 tokens firmados con la clave secreta: **access** (60 min, lleva correo y rol) y **refresh** (7 días).
3. El frontend envía el access token en cada petición: `Authorization: Bearer <token>`.
4. `JwtAuthFilter` (se ejecuta en cada petición) valida firma y vencimiento y le dice a Spring Security quién es y qué rol tiene.
5. `SecurityConfig` decide por ruta y rol: `.requestMatchers("/v1/usuarios/**").hasRole("ADMINISTRADOR")`.
6. Si el token vence, el frontend llama `POST /v1/auth/refresh` con el refresh token y recibe tokens nuevos.

**¿Qué es un JWT?** Tres partes `cabecera.datos.firma`. Los datos (correo, rol, vencimiento) van en texto legible, pero la **firma** impide modificarlos: si alguien cambia el rol a ADMINISTRADOR, la firma ya no coincide y el servidor lo rechaza.

**Seguridad aplicada (puntos para nombrar):**
- Contraseñas con BCrypt (hash de una vía), nunca en texto plano ni en las respuestas (`@JsonProperty(access = WRITE_ONLY)`).
- Clave de firma en una variable de entorno, mínimo 32 caracteres (si es más corta la app no arranca).
- Tokens con vencimiento; el refresh token **no** sirve como access token (campo `tipo`).
- Al refrescar se vuelve a consultar la BD: si el usuario fue desactivado, ya no renueva.
- Sin sesión en el servidor (*stateless*) y CSRF desactivado porque no usamos cookies.
- El registro público siempre crea `CLIENTE` (nadie puede auto-asignarse admin).
- **401** = no sabemos quién eres (sin token o token inválido). **403** = sabemos quién eres pero tu rol no puede hacer eso.

**Roles (pauta):** ADMINISTRADOR (todo) · VENDEDOR (solo ver productos y boletas) · CLIENTE (solo la tienda). La tabla completa está en el `README.md`.

---

## 4. Integración backend ↔ frontend (IE3.2.2 / IE3.2.4) — *pendiente en el frontend*

**Flujo de datos para explicar:**
`React (fetch/axios + header Authorization)` → `API REST /v1/...` → `Controller → Service → Repository` → `MySQL` → respuesta JSON → `React actualiza la pantalla`.

- **CORS:** el navegador bloquea llamadas entre orígenes distintos (S3 → EC2). `CorsConfig` permite solo el origen de `CORS_ORIGIN`.
- La URL del backend se fija al compilar el frontend (`VITE_API_URL`).

## 5. Sesión en el frontend (IE3.3.2 / IE3.3.5) — *pendiente*

Plan: al hacer login guardar `token`, `refreshToken` y el usuario (`localStorage`); al recargar la página leerlos de nuevo; si una petición responde 401, intentar `/v1/auth/refresh` y si falla, cerrar sesión.
Para explicar los riesgos: `localStorage` es accesible por JavaScript (riesgo XSS); la alternativa más segura es una cookie `HttpOnly`.

## 6. Restricciones de acceso en el frontend (IE3.3.3 / IE3.3.6) — *pendiente*

Plan: un componente `RutaProtegida` que revise el rol guardado y redirija; el menú muestra solo lo que corresponde (el vendedor solo ve productos y boletas).
Idea clave para decir en la presentación: **ocultar botones en el frontend es solo comodidad; la seguridad real está en el backend** (aunque alguien fuerce la URL, el backend responde 403).

---

## Preguntas probables (respuestas cortas)

- **¿Para qué sirven los DTO?** Para definir qué datos acepta cada endpoint y validarlos (ej. el cliente no puede enviar el precio ni el rol).
- **¿Por qué el total lo calcula el servidor?** Si lo enviara el cliente, podría mandar un total falso.
- **¿Qué pasa si dos clientes compran el último producto a la vez?** En esta versión la transacción valida el stock, pero no hay bloqueo entre transacciones; una mejora sería bloqueo pesimista u optimista (`@Version`). *(Conviene reconocer esta limitación.)*
- **¿Cómo se cierra la sesión?** El JWT no se puede "borrar" del servidor (es stateless); el frontend descarta los tokens y estos vencen solos.
- **¿Por qué `ddl-auto=update` y no migraciones?** Simplicidad para el curso; en producción real se usaría Flyway o Liquibase.
- **¿Para qué sirve `@Transactional`?** Para que un pedido se guarde completo o no se guarde nada.
- **¿Qué es `@Autowired`/inyección de dependencias?** Spring crea los objetos (services, repositories) y los entrega por el constructor; aquí usamos inyección por constructor.
- **¿Por qué H2 en desarrollo y MySQL en AWS?** Con H2 se desarrolla sin instalar nada; el perfil `prod` cambia solo la conexión por variables de entorno, el mismo JAR corre en ambos.

## Limitaciones conocidas (conviene mencionarlas con honestidad)

- Sin bloqueo de concurrencia en el stock (ver arriba).
- Los usuarios de demostración existen hasta que se ponga `DEMO_USERS=false`.
- Sin límite de intentos de login.
- HTTP sin cifrar en el laboratorio de AWS.
