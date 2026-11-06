-- Modelo relacional: USUARIO -> BOLETA -> DETALLE_BOLETA <- PRODUCTO <- CATEGORIA
-- (Anexo 1, Figura 1). Compatible con MySQL 8 (AWS RDS) y H2 en modo MySQL.

CREATE TABLE categoria (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL,
    descripcion VARCHAR(255),
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
);

CREATE TABLE producto (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo       VARCHAR(40)  NOT NULL,
    nombre       VARCHAR(120) NOT NULL,
    descripcion  VARCHAR(255),
    precio       INT          NOT NULL,
    stock        INT          NOT NULL DEFAULT 0,
    stock_minimo INT          NOT NULL DEFAULT 0,
    estado       VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
    categoria_id BIGINT       NOT NULL,
    CONSTRAINT uk_producto_codigo UNIQUE (codigo),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    CONSTRAINT ck_producto_stock CHECK (stock >= 0)
);

CREATE TABLE usuario (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    correo         VARCHAR(120) NOT NULL,
    telefono       VARCHAR(20),
    direccion      VARCHAR(200),
    password_hash  VARCHAR(100) NOT NULL,
    rol            VARCHAR(20)  NOT NULL,
    estado         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP    NOT NULL,
    CONSTRAINT uk_usuario_correo UNIQUE (correo)
);

CREATE TABLE boleta (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT       NOT NULL,
    repartidor_id    BIGINT,
    nombre_cliente   VARCHAR(100) NOT NULL,
    telefono         VARCHAR(20)  NOT NULL,
    direccion        VARCHAR(200) NOT NULL,
    fecha            TIMESTAMP    NOT NULL,
    estado           VARCHAR(15)  NOT NULL DEFAULT 'PENDIENTE',
    total            INT          NOT NULL,
    estado_pago      VARCHAR(10)  NOT NULL DEFAULT 'PENDIENTE',
    metodo_pago      VARCHAR(20),
    monto_pagado     INT,
    fecha_pago       TIMESTAMP NULL,
    CONSTRAINT fk_boleta_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_boleta_repartidor FOREIGN KEY (repartidor_id) REFERENCES usuario (id)
);

CREATE INDEX idx_boleta_estado ON boleta (estado);

CREATE TABLE detalle_boleta (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    boleta_id       BIGINT NOT NULL,
    producto_id     BIGINT NOT NULL,
    cantidad        INT    NOT NULL,
    precio_unitario INT    NOT NULL,
    subtotal        INT    NOT NULL,
    CONSTRAINT fk_detalle_boleta FOREIGN KEY (boleta_id) REFERENCES boleta (id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0)
);
