# =====================================================================
# Dockerfile del backend DisGas (mismo patron que el backend del profesor)
# Se construye en 2 etapas: primero se COMPILA, luego se EJECUTA.
# La imagen final solo lleva Java + el JAR (no lleva Maven ni el codigo fuente).
# =====================================================================

# ---------- Etapa 1: compilar con Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# Se copia solo el pom.xml y se descargan las librerias.
# Si despues solo cambias codigo, Docker reutiliza esta capa y el build es mucho mas rapido.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true

# Ahora si se copia el codigo y se genera el JAR (target/app.jar, el nombre sale del pom: <finalName>)
# -Dmaven.test.skip=true: no compila ni ejecuta las pruebas dentro de la imagen (las pruebas se corren con ./mvnw test)
COPY src ./src
RUN mvn -q -B clean package -Dmaven.test.skip=true

# ---------- Etapa 2: ejecutar solo con Java ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Se trae unicamente el JAR desde la etapa anterior
COPY --from=build /src/target/app.jar app.jar

# Por seguridad la app NO corre como administrador (root) dentro del contenedor
RUN useradd --system --no-create-home spring
USER spring

# Puerto en el que escucha Spring Boot (documentativo; se publica en docker-compose.yml)
EXPOSE 8080

# -Xmx512m limita la memoria de Java (igual que en el servicio de EC2 del taller)
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
