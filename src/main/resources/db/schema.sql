CREATE TABLE IF NOT EXISTS usuario (
  id_usuario SERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  telefono VARCHAR(15) UNIQUE NOT NULL,
  email VARCHAR(100) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL
);
CREATE TABLE IF NOT EXISTS categoria (
  id_categoria SERIAL PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL,
  id_usuario INT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS producto (
  id_producto SERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  id_categoria INT NOT NULL REFERENCES categoria(id_categoria),
  id_usuario INT NOT NULL REFERENCES usuario(id_usuario),
  precio DECIMAL(10,2) NOT NULL CHECK (precio > 0),
  stock DECIMAL(10,3) DEFAULT 0 CHECK (stock >= 0),
  unidad_medida VARCHAR(10) NOT NULL DEFAULT 'unidad',
  estado VARCHAR(10) DEFAULT 'verde' CHECK (estado IN ('verde','amarillo','rojo')),
  stock_minimo DECIMAL(10,3) DEFAULT 3,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL
);
CREATE TABLE IF NOT EXISTS venta (
  id_venta SERIAL PRIMARY KEY,
  id_usuario INT NOT NULL REFERENCES usuario(id_usuario),
  fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  total DECIMAL(10,2) NOT NULL CHECK (total > 0)
);
CREATE TABLE IF NOT EXISTS detalle_venta (
  id_detalle SERIAL PRIMARY KEY,
  id_venta INT NOT NULL REFERENCES venta(id_venta) ON DELETE CASCADE,
  id_producto INT NOT NULL REFERENCES producto(id_producto),
  cantidad DECIMAL(10,3) NOT NULL CHECK (cantidad > 0),
  precio_unitario DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS auditoria (
  id_auditoria SERIAL PRIMARY KEY,
  id_usuario INT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
  accion VARCHAR(20) NOT NULL CHECK (accion IN ('INSERT','UPDATE','DELETE','LOGIN','LOGOUT')),
  tabla_afectada VARCHAR(50) NOT NULL,
  fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  detalles TEXT
);
CREATE TABLE IF NOT EXISTS recordatorio (
  id_recordatorio SERIAL PRIMARY KEY,
  id_producto INT NOT NULL REFERENCES producto(id_producto) ON DELETE CASCADE,
  tipo_alerta VARCHAR(20) NOT NULL CHECK (tipo_alerta IN ('poco_stock','agotado')),
  mensaje TEXT NOT NULL,
  leido BOOLEAN DEFAULT FALSE,
  fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS historial_stock (
  id_historial SERIAL PRIMARY KEY,
  id_producto INT NOT NULL REFERENCES producto(id_producto),
  id_usuario INT NOT NULL REFERENCES usuario(id_usuario),
  stock_anterior DECIMAL(10,3) NOT NULL,
  stock_nuevo DECIMAL(10,3) NOT NULL,
  tipo_movimiento VARCHAR(20) NOT NULL CHECK (tipo_movimiento IN ('venta','reposicion','ajuste','merma')),
  fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_producto_usuario ON producto(id_usuario);
CREATE INDEX IF NOT EXISTS idx_producto_estado  ON producto(estado);
CREATE INDEX IF NOT EXISTS idx_venta_fecha      ON venta(fecha);
CREATE INDEX IF NOT EXISTS idx_detalle_venta    ON detalle_venta(id_venta);
CREATE UNIQUE INDEX IF NOT EXISTS uq_categoria_usuario_nombre ON categoria(id_usuario, lower(nombre));

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO barrio;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO barrio;
GRANT USAGE, CREATE ON SCHEMA public TO barrio;


