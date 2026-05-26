-- ============================================================
-- SCHEMA - Sistema Barbería Juan
-- MySQL 8.0+
-- Ejecutar como: mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS barberia_juan
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE barberia_juan;

-- Crear usuario de aplicación con privilegios mínimos
CREATE USER IF NOT EXISTS 'barberia_app'@'localhost' IDENTIFIED BY 'B@rb3r1a_S3cure_2024!';
GRANT SELECT, INSERT, UPDATE, DELETE ON barberia_juan.* TO 'barberia_app'@'localhost';
FLUSH PRIVILEGES;

-- ============================================================
-- TABLA: usuarios (administradores y barberos del sistema)
-- ============================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt hash',
    nombre      VARCHAR(100) NOT NULL,
    apellido    VARCHAR(100) NOT NULL,
    telefono    VARCHAR(20),
    email       VARCHAR(100),
    rol         ENUM('ADMIN', 'BARBERO') NOT NULL DEFAULT 'BARBERO',
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    ultimo_login DATETIME,
    INDEX idx_username (username),
    INDEX idx_rol (rol)
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: clientes
-- ============================================================
CREATE TABLE IF NOT EXISTS clientes (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    apellido        VARCHAR(100) NOT NULL,
    telefono        VARCHAR(30) NOT NULL COMMENT 'Número WhatsApp con código país ej: +52...',
    email           VARCHAR(100),
    notas           TEXT COMMENT 'Notas del cliente (preferencias, etc.)',
    fecha_registro  DATETIME DEFAULT CURRENT_TIMESTAMP,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_telefono (telefono),
    INDEX idx_nombre (nombre, apellido),
    INDEX idx_telefono (telefono)
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: servicios
-- ============================================================
CREATE TABLE IF NOT EXISTS servicios (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    descripcion     TEXT,
    precio          DECIMAL(10,2) NOT NULL,
    duracion_minutos INT NOT NULL DEFAULT 30 COMMENT 'Duración del servicio en minutos',
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activo (activo)
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: citas
-- ============================================================
CREATE TABLE IF NOT EXISTS citas (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id      INT NOT NULL,
    servicio_id     INT NOT NULL,
    barbero_id      INT COMMENT 'Usuario con rol BARBERO asignado',
    fecha_hora      DATETIME NOT NULL,
    fecha_fin       DATETIME NOT NULL COMMENT 'Calculado: fecha_hora + duracion_servicio',
    estado          ENUM('PENDIENTE', 'CONFIRMADA', 'EN_PROCESO', 'COMPLETADA', 'CANCELADA', 'NO_ASISTIO')
                    NOT NULL DEFAULT 'PENDIENTE',
    notas           TEXT,
    canal_reserva   ENUM('WHATSAPP', 'PANEL', 'TELEFONO') DEFAULT 'WHATSAPP',
    notificado      BOOLEAN DEFAULT FALSE COMMENT 'Si se envió notificación de confirmación',
    fecha_creacion  DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    FOREIGN KEY (servicio_id) REFERENCES servicios(id) ON DELETE RESTRICT,
    FOREIGN KEY (barbero_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    INDEX idx_fecha_hora (fecha_hora),
    INDEX idx_estado (estado),
    INDEX idx_cliente (cliente_id),
    INDEX idx_barbero (barbero_id),
    INDEX idx_fecha_estado (fecha_hora, estado)
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: pagos
-- ============================================================
CREATE TABLE IF NOT EXISTS pagos (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    cita_id         INT NOT NULL,
    monto           DECIMAL(10,2) NOT NULL,
    metodo_pago     ENUM('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'OTRO') NOT NULL DEFAULT 'EFECTIVO',
    referencia      VARCHAR(100) COMMENT 'Número de referencia/transacción',
    notas           TEXT,
    fecha_pago      DATETIME DEFAULT CURRENT_TIMESTAMP,
    registrado_por  INT COMMENT 'Usuario que registró el pago',
    FOREIGN KEY (cita_id) REFERENCES citas(id) ON DELETE RESTRICT,
    FOREIGN KEY (registrado_por) REFERENCES usuarios(id) ON DELETE SET NULL,
    INDEX idx_cita (cita_id),
    INDEX idx_fecha_pago (fecha_pago)
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: sesiones_chatbot (estado de la conversación WhatsApp)
-- ============================================================
CREATE TABLE IF NOT EXISTS sesiones_chatbot (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    telefono        VARCHAR(30) NOT NULL UNIQUE,
    estado          VARCHAR(50) NOT NULL DEFAULT 'INICIO',
    datos_sesion    JSON COMMENT 'Datos temporales de la conversación en curso',
    ultima_actividad DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_telefono (telefono),
    INDEX idx_ultima_actividad (ultima_actividad)
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: configuracion (parámetros del sistema)
-- ============================================================
CREATE TABLE IF NOT EXISTS configuracion (
    clave       VARCHAR(100) PRIMARY KEY,
    valor       TEXT NOT NULL,
    descripcion VARCHAR(255),
    fecha_modificacion DATETIME ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
