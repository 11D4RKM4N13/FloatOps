package com.floatops.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.floatops.dto.generated.AuditLog;
import com.floatops.dto.generated.ListAuditLogs200Response;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

// RF-027: registro automatico de auditoria. No hay un POST publico (ver
// ApiContract/openapi-auditoria.yaml) - otros servicios llaman a
// registrar(...) desde dentro de su propia transaccion (WorkOrderService,
// y mas adelante VehicleService cuando exista el cambio de estado).
@ApplicationScoped
public class AuditService {

    // JsonWebToken lo provee quarkus-oidc para tokens bearer; si no hay
    // request autenticado (tests sin @TestSecurity, o llamadas internas
    // futuras sin usuario) sus getters devuelven null en vez de lanzar.
    @Inject
    JsonWebToken jwt;

    @Transactional
    public void registrar(String entidadAfectada, UUID entidadId, String campo, Object valorAnterior, Object valorNuevo) {
        com.floatops.entity.AuditLog log = new com.floatops.entity.AuditLog();
        log.entidadAfectada = entidadAfectada;
        log.entidadId = entidadId;
        log.campo = campo;
        log.valorAnterior = valorAnterior != null ? valorAnterior.toString() : null;
        log.valorNuevo = valorNuevo != null ? valorNuevo.toString() : null;
        log.userId = currentUserId();
        log.timestamp = LocalDateTime.now();
        log.persist();
    }

    // El claim "sub" de Keycloak es el UUID estable del usuario, a
    // diferencia del principal expuesto por SecurityIdentity.getName()
    // (que puede mapear a preferred_username segun configuracion de OIDC).
    private UUID currentUserId() {
        if (jwt == null) {
            return null;
        }
        String sub = jwt.getSubject();
        if (sub == null) {
            return null;
        }
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public ListAuditLogs200Response listar(String entidadAfectada, UUID entidadId, UUID userId,
                                            LocalDate from, LocalDate to, Integer page, Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        List<com.floatops.entity.AuditLog> registros =
                com.floatops.entity.AuditLog.buscar(entidadAfectada, entidadId, userId, from, to, pageNum, pageSize);
        long total = com.floatops.entity.AuditLog.contar(entidadAfectada, entidadId, userId, from, to);

        List<AuditLog> items = registros.stream().map(this::toApi).collect(Collectors.toList());

        ListAuditLogs200Response response = new ListAuditLogs200Response();
        response.setItems(items);
        response.setTotal((int) total);
        return response;
    }

    private AuditLog toApi(com.floatops.entity.AuditLog log) {
        AuditLog dto = new AuditLog();
        dto.setId(log.id);
        dto.setEntidadAfectada(log.entidadAfectada);
        dto.setEntidadId(log.entidadId);
        dto.setCampo(log.campo);
        dto.setValorAnterior(log.valorAnterior);
        dto.setValorNuevo(log.valorNuevo);
        dto.setUserId(log.userId);
        dto.setTimestamp(log.timestamp.atOffset(ZoneOffset.UTC));
        return dto;
    }
}
