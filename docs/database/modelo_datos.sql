-- =====================================================================
-- MODELO DE DATOS DisGas (MySQL 8) - SCRIPT DE REFERENCIA
--
-- IMPORTANTE: la aplicacion NO necesita que ejecutes este archivo.
-- Hibernate crea estas mismas tablas solo, a partir de las clases del paquete "model"
-- (spring.jpa.hibernate.ddl-auto=update). Este script sirve para:
--   * documentar el modelo (Figura 1 de la pauta: USUARIO -> BOLETA -> DETALLE_BOLETA <- PRODUCTO <- CATEGORIA)
--   * importarlo en SQL Developer Data Modeler o MySQL Workbench y obtener el diagrama
--     (Data Modeler: File > Import > Data Modeler > DDL File;  Workbench: File > New Model > File > Import > Reverse Engineer SQL Script)
--
-- Los tipos pueden variar en detalles menores respecto de lo que genera Hibernate.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS disgas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE disgas;

-- ---------- CATEGORIA: agrupa los productos (Gas, Accesorios) ----------
CREATE TABLE categoria (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categoria_nombre (nombre)               -- no pueden existir dos categorias con el mismo nombre
);

-- ---------- PRODUCTO: lo que se vende; tambien es el inventario (stock) ----------
CREATE TABLE producto (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(255) NOT NULL,
    descripcion  VARCHAR(255),
    precio       INT          NOT NULL,                   -- pesos chilenos, sin decimales
    stock        INT          NOT NULL,                   -- unidades disponibles (baja al comprar)
    stock_minimo INT          NOT NULL,                   -- si stock <= stock_minimo, hay que reponer
    activo       BIT          NOT NULL,                   -- 0 = oculto en la tienda
    categoria_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
);

-- ---------- USUARIO: clientes, vendedores y administradores ----------
CREATE TABLE usuario (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    nombre    VARCHAR(255) NOT NULL,
    correo    VARCHAR(255) NOT NULL,
    telefono  VARCHAR(255),
    direccion VARCHAR(255),
    password  VARCHAR(255) NOT NULL,                      -- hash BCrypt, nunca la clave real
    rol       ENUM('ADMINISTRADOR','VENDEDOR','CLIENTE') NOT NULL,
    activo    BIT          NOT NULL,                      -- 0 = cuenta desactivada (no puede iniciar sesion)
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_correo (correo)                 -- el correo identifica al usuario al iniciar sesion
);

-- ---------- BOLETA: el pedido de un cliente ----------
CREATE TABLE boleta (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT       NOT NULL,                     -- quien compro
    fecha      DATETIME(6)  NOT NULL,
    direccion  VARCHAR(255) NOT NULL,                     -- direccion de despacho de este pedido
    estado     ENUM('PENDIENTE','EN_RUTA','ENTREGADO','CANCELADO') NOT NULL,
    total      INT          NOT NULL,                     -- lo calcula el servidor
    PRIMARY KEY (id),
    CONSTRAINT fk_boleta_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

-- ---------- DETALLE_BOLETA: cada linea de la boleta ----------
CREATE TABLE detalle_boleta (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    boleta_id       BIGINT NOT NULL,
    producto_id     BIGINT NOT NULL,
    cantidad        INT    NOT NULL,
    precio_unitario INT    NOT NULL,                      -- precio al momento de comprar (no cambia si el producto sube)
    subtotal        INT    NOT NULL,                      -- cantidad * precio_unitario
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_boleta   FOREIGN KEY (boleta_id)   REFERENCES boleta (id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);
