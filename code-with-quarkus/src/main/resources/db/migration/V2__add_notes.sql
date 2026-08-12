-- V2__add_notes.sql
-- RF-014: notas de texto libre de la vista movil del tecnico.
-- Vacio de diseno original - nunca se modelo en V1__init.sql.

CREATE TABLE notes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    texto         TEXT NOT NULL,
    timestamp     TIMESTAMP NOT NULL,
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_notes_work_order ON notes(work_order_id);