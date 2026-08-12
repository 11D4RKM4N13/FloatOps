-- V1__init.sql
-- Tablas del nucleo de Ordenes de trabajo + stubs minimos de Vehicle/Technician.
--
-- IDs como UUID (no BIGSERIAL/Long) para coincidir con los 11 contratos
-- OpenAPI, que definen todos los ids como "string, format: uuid".
-- Hibernate genera el UUID del lado de la aplicacion (GenerationType.UUID
-- en BaseEntity.java); gen_random_uuid() aqui es solo para poder insertar
-- filas directo por SQL (como el seed de stages mas abajo) sin pasar por Java.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE service_types (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE part_categories (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE parts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo            VARCHAR(50) NOT NULL UNIQUE,
    nombre            VARCHAR(150) NOT NULL,
    stock             INTEGER NOT NULL DEFAULT 0,
    precio            NUMERIC(12,2) NOT NULL,
    part_category_id  UUID REFERENCES part_categories(id)
);

CREATE TABLE stages (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    orden  INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true
);

-- RF-035: configuracion inicial por defecto, NO un limite fijo.
INSERT INTO stages (nombre, orden, activo) VALUES
    ('Requiere atencion', 1, true),
    ('Programado', 2, true),
    ('En taller', 3, true),
    ('Completados', 4, true);

-- STUBS minimos: se amplian con ALTER TABLE en las migraciones de sus modulos
CREATE TABLE vehicles (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    placa  VARCHAR(20) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
);

CREATE TABLE technicians (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL
);

CREATE TABLE work_orders (
    id                             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prioridad                      VARCHAR(10) NOT NULL,
    descripcion                    TEXT NOT NULL,
    fecha_programada               DATE,
    presupuesto                    NUMERIC(12,2),
    cotizacion_aprobada_cliente    BOOLEAN NOT NULL DEFAULT false,
    cotizacion_aprobada_supervisor BOOLEAN,
    lista_para_revision            BOOLEAN NOT NULL DEFAULT false,
    vehicle_id                     UUID NOT NULL REFERENCES vehicles(id),
    technician_id                  UUID REFERENCES technicians(id),
    stage_id                       UUID NOT NULL REFERENCES stages(id),
    service_type_id                UUID NOT NULL REFERENCES service_types(id)
);

CREATE TABLE checklist_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    descripcion   VARCHAR(255) NOT NULL,
    completado    BOOLEAN NOT NULL DEFAULT false,
    orden         INTEGER NOT NULL DEFAULT 0,
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE
);

CREATE TABLE photos (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url           VARCHAR(500) NOT NULL,
    timestamp     TIMESTAMP NOT NULL,
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE
);

CREATE TABLE work_order_parts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cantidad      INTEGER NOT NULL DEFAULT 1,
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    part_id       UUID NOT NULL REFERENCES parts(id)
);

CREATE INDEX idx_work_orders_stage ON work_orders(stage_id);
CREATE INDEX idx_work_orders_technician ON work_orders(technician_id);
CREATE INDEX idx_work_orders_vehicle ON work_orders(vehicle_id);