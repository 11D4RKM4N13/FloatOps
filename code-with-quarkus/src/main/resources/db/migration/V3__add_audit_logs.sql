-- V3__add_audit_logs.sql
-- RF-027: trazabilidad de cambios. Sin FK a una tabla de usuarios porque
-- el modulo de Administracion (User) todavia no existe - user_id guarda
-- el claim "sub" (UUID) del JWT de Keycloak, sin restriccion referencial
-- por ahora (igual que vehicles/technicians arrancaron como stubs).

CREATE TABLE audit_logs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entidad_afectada  VARCHAR(100) NOT NULL,
    entidad_id        UUID NOT NULL,
    campo             VARCHAR(100) NOT NULL,
    valor_anterior    TEXT,
    valor_nuevo       TEXT,
    user_id           UUID,
    timestamp         TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_logs_entidad ON audit_logs(entidad_afectada, entidad_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
