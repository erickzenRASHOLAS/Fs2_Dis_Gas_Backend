-- Datos de referencia: catálogo inicial de Gas El Volcán (tomado del frontend).

INSERT INTO categoria (nombre, descripcion) VALUES ('Gas', 'Cilindros de gas licuado');
INSERT INTO categoria (nombre, descripcion) VALUES ('Accesorios', 'Conectores y accesorios para gas');

INSERT INTO producto (codigo, nombre, descripcion, precio, stock, stock_minimo, estado, categoria_id)
VALUES ('gas-5', 'Cilindro 5 kg', 'Cilindro de gas licuado de 5 kg', 10800, 32, 8, 'ACTIVO', (SELECT id FROM categoria WHERE nombre = 'Gas'));
INSERT INTO producto (codigo, nombre, descripcion, precio, stock, stock_minimo, estado, categoria_id)
VALUES ('gas-11', 'Cilindro 11 kg', 'Cilindro de gas licuado de 11 kg', 21500, 48, 10, 'ACTIVO', (SELECT id FROM categoria WHERE nombre = 'Gas'));
INSERT INTO producto (codigo, nombre, descripcion, precio, stock, stock_minimo, estado, categoria_id)
VALUES ('gas-15', 'Cilindro 15 kg', 'Cilindro de gas licuado de 15 kg', 27900, 27, 8, 'ACTIVO', (SELECT id FROM categoria WHERE nombre = 'Gas'));

INSERT INTO producto (codigo, nombre, descripcion, precio, stock, stock_minimo, estado, categoria_id)
VALUES ('acc-regulador', 'Regulador de gas', 'Regulador de presión para cilindro', 12500, 40, 10, 'ACTIVO', (SELECT id FROM categoria WHERE nombre = 'Accesorios'));
INSERT INTO producto (codigo, nombre, descripcion, precio, stock, stock_minimo, estado, categoria_id)
VALUES ('acc-manguera', 'Manguera para gas 1,5 metros', 'Manguera flexible para gas de 1,5 m', 5500, 40, 10, 'ACTIVO', (SELECT id FROM categoria WHERE nombre = 'Accesorios'));
INSERT INTO producto (codigo, nombre, descripcion, precio, stock, stock_minimo, estado, categoria_id)
VALUES ('acc-conector', 'Conector de gas', 'Conector para instalación de gas', 3500, 60, 10, 'ACTIVO', (SELECT id FROM categoria WHERE nombre = 'Accesorios'));
INSERT INTO producto (codigo, nombre, descripcion, precio, stock, stock_minimo, estado, categoria_id)
VALUES ('acc-abrazadera', 'Abrazadera para manguera', 'Abrazadera metálica para manguera', 1200, 80, 15, 'ACTIVO', (SELECT id FROM categoria WHERE nombre = 'Accesorios'));
INSERT INTO producto (codigo, nombre, descripcion, precio, stock, stock_minimo, estado, categoria_id)
VALUES ('acc-kit', 'Kit regulador y manguera', 'Kit completo de regulador y manguera', 16900, 25, 5, 'ACTIVO', (SELECT id FROM categoria WHERE nombre = 'Accesorios'));
