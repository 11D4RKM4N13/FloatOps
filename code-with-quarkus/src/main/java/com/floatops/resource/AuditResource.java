package com.floatops.resource;

import java.time.LocalDate;
import java.util.UUID;

import com.floatops.dto.generated.ListAuditLogs200Response;
import com.floatops.resource.generated.AuditLogsApi;
import com.floatops.service.AuditService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

// Implementa la interfaz generada desde ApiContract/openapi-auditoria.yaml.
// Solo lectura - el registro es automatico, disparado por otros servicios
// (ver AuditService.registrar), no hay endpoint publico para crear entradas.
public class AuditResource implements AuditLogsApi {

    @Inject
    AuditService service;

    @Override
    @RolesAllowed({"administrador"}) // unico rol con pantalla de auditoria (README seccion 6.3)
    public ListAuditLogs200Response listAuditLogs(String entidadAfectada, UUID entidadId, UUID userId,
                                                    LocalDate from, LocalDate to, Integer page, Integer size) {
        return service.listar(entidadAfectada, entidadId, userId, from, to, page, size);
    }
}
