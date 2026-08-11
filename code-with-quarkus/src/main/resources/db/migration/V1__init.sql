-- V1__init.sql
-- Tablas del nucleo de Ordenes de trabajo + stubs minimos de Vehicle/Technician.
--
-- IMPORTANTE: PanacheEntity (Java) genera IDs con una secuencia nombrada
-- explicitamente "{nombre_tabla}_SEQ" (no la secuencia implicita que crea
-- BIGSERIAL, que se llama "{tabla}_id_seq"). Por eso aca creamos cada
-- secuencia a mano con el nombre exacto que Hibernate espera, en vez de
-- usar BIGSERIAL.

CREATE SEQUENCE service_types_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE service_types (
    id     BIGINT PRIMARY KEY DEFAULT nextval('service_types_SEQ'),
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE SEQUENCE part_categories_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE part_categories (
    id     BIGINT PRIMARY KEY DEFAULT nextval('part_categories_SEQ'),
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE SEQUENCE parts_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE parts (
    id                BIGINT PRIMARY KEY DEFAULT nextval('parts_SEQ'),
    codigo            VARCHAR(50) NOT NULL UNIQUE,
    nombre            VARCHAR(150) NOT NULL,
    stock             INTEGER NOT NULL DEFAULT 0,
    precio            NUMERIC(12,2) NOT NULL,
    part_category_id  BIGINT REFERENCES part_categories(id)
);

CREATE SEQUENCE stages_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE stages (
    id     BIGINT PRIMARY KEY DEFAULT nextval('stages_SEQ'),
    nombre VARCHAR(100) NOT NULL,
    orden  INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true
);

-- RF-035: configuracion inicial por defecto, NO un limite fijo.
-- El Administrador puede agregar, renombrar o reordenar despues (RF-035 / RF-040).
INSERT INTO stages (nombre, orden, activo) VALUES
    ('Requiere atencion', 1, true),
    ('Programado', 2, true),
    ('En taller', 3, true),
    ('Completados', 4, true);

-- STUBS minimos: se amplian con ALTER TABLE en las migraciones de sus modulos
CREATE SEQUENCE vehicles_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE vehicles (
    id     BIGINT PRIMARY KEY DEFAULT nextval('vehicles_SEQ'),
    placa  VARCHAR(20) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
);

CREATE SEQUENCE technicians_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE technicians (
    id     BIGINT PRIMARY KEY DEFAULT nextval('technicians_SEQ'),
    nombre VARCHAR(150) NOT NULL
);

CREATE SEQUENCE work_orders_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE work_orders (
    id                             BIGINT PRIMARY KEY DEFAULT nextval('work_orders_SEQ'),
    prioridad                      VARCHAR(10) NOT NULL,
    descripcion                    TEXT NOT NULL,
    fecha_programada               DATE,
    presupuesto                    NUMERIC(12,2),
    cotizacion_aprobada_cliente    BOOLEAN NOT NULL DEFAULT false,
    cotizacion_aprobada_supervisor BOOLEAN,
    lista_para_revision            BOOLEAN NOT NULL DEFAULT false,
    vehicle_id                     BIGINT NOT NULL REFERENCES vehicles(id),
    technician_id                  BIGINT REFERENCES technicians(id),
    stage_id                       BIGINT NOT NULL REFERENCES stages(id),
    service_type_id                BIGINT NOT NULL REFERENCES service_types(id)
);

CREATE SEQUENCE checklist_items_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE checklist_items (
    id            BIGINT PRIMARY KEY DEFAULT nextval('checklist_items_SEQ'),
    descripcion   VARCHAR(255) NOT NULL,
    completado    BOOLEAN NOT NULL DEFAULT false,
    orden         INTEGER NOT NULL DEFAULT 0,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE
);

CREATE SEQUENCE photos_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE photos (
    id            BIGINT PRIMARY KEY DEFAULT nextval('photos_SEQ'),
    url           VARCHAR(500) NOT NULL,
    timestamp     TIMESTAMP NOT NULL,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE
);

CREATE SEQUENCE work_order_parts_SEQ START WITH 1 INCREMENT BY 50;
CREATE TABLE work_order_parts (
    id            BIGINT PRIMARY KEY DEFAULT nextval('work_order_parts_SEQ'),
    cantidad      INTEGER NOT NULL DEFAULT 1,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    part_id       BIGINT NOT NULL REFERENCES parts(id)
);

-- Indices para las consultas mas frecuentes (RF-004 kanban, RF-032 reasignacion, RF-037 cola)
CREATE INDEX idx_work_orders_stage ON work_orders(stage_id);
CREATE INDEX idx_work_orders_technician ON work_orders(technician_id);
CREATE INDEX idx_work_orders_vehicle ON work_orders(vehicle_id);