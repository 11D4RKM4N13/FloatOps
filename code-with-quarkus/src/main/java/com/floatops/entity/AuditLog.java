package com.floatops.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_entidad", columnList = "entidad_afectada, entidad_id"),
        @Index(name = "idx_audit_logs_user", columnList = "user_id"),
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp")
})
public class AuditLog extends BaseEntity {

    @Column(name = "entidad_afectada", nullable = false)
    public String entidadAfectada;

    @Column(name = "entidad_id", nullable = false)
    public UUID entidadId;

    @Column(nullable = false)
    public String campo;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    public String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    public String valorNuevo;

    // Nullable: no siempre hay un usuario autenticado detras del cambio
    // (ej. jobs internos futuros). Guarda el claim "sub" del JWT de
    // Keycloak (UUID estable del usuario), no el username - todavia no
    // existe una tabla local de usuarios (modulo de Administracion sin
    // implementar) para tener un FK real.
    @Column(name = "user_id")
    public UUID userId;

    @Column(nullable = false)
    public LocalDateTime timestamp;

    public static List<AuditLog> buscar(String entidadAfectada, UUID entidadId, UUID userId,
                                         LocalDate from, LocalDate to, int page, int size) {
        StringBuilder query = new StringBuilder("1 = 1");
        List<Object> params = new java.util.ArrayList<>();
        int idx = 1;

        if (entidadAfectada != null) {
            query.append(" and entidadAfectada = ?").append(idx++);
            params.add(entidadAfectada);
        }
        if (entidadId != null) {
            query.append(" and entidadId = ?").append(idx++);
            params.add(entidadId);
        }
        if (userId != null) {
            query.append(" and userId = ?").append(idx++);
            params.add(userId);
        }
        if (from != null) {
            query.append(" and timestamp >= ?").append(idx++);
            params.add(from.atStartOfDay());
        }
        if (to != null) {
            query.append(" and timestamp < ?").append(idx++);
            params.add(to.plusDays(1).atStartOfDay());
        }

        return find(query.toString() + " order by timestamp desc", params.toArray())
                .page(page, size)
                .list();
    }

    public static long contar(String entidadAfectada, UUID entidadId, UUID userId,
                               LocalDate from, LocalDate to) {
        StringBuilder query = new StringBuilder("1 = 1");
        List<Object> params = new java.util.ArrayList<>();
        int idx = 1;

        if (entidadAfectada != null) {
            query.append(" and entidadAfectada = ?").append(idx++);
            params.add(entidadAfectada);
        }
        if (entidadId != null) {
            query.append(" and entidadId = ?").append(idx++);
            params.add(entidadId);
        }
        if (userId != null) {
            query.append(" and userId = ?").append(idx++);
            params.add(userId);
        }
        if (from != null) {
            query.append(" and timestamp >= ?").append(idx++);
            params.add(from.atStartOfDay());
        }
        if (to != null) {
            query.append(" and timestamp < ?").append(idx++);
            params.add(to.plusDays(1).atStartOfDay());
        }

        return find(query.toString(), params.toArray()).count();
    }
}
