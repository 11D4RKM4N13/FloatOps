package com.floatops.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.floatops.dto.generated.AuditLog;
import com.floatops.dto.generated.ListAuditLogs200Response;
import com.floatops.entity.Prioridad;
import com.floatops.entity.ServiceType;
import com.floatops.entity.Stage;
import com.floatops.entity.Vehicle;
import com.floatops.entity.WorkOrder;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-027: AuditService.registrar() es de uso interno (llamado por otros
 * servicios dentro de su propia transaccion, ver WorkOrderService), no
 * hay endpoint publico para crear entradas - por eso se prueba
 * directamente contra el servicio, sin pasar por HTTP.
 */
@QuarkusTest
class AuditServiceTest {

    @Inject
    AuditService service;

    private WorkOrder createWorkOrder() {
        Vehicle vehicle = new Vehicle();
        vehicle.placa = "AUD-" + UUID.randomUUID().toString().substring(0, 6);
        vehicle.persist();

        Stage stage = new Stage();
        stage.nombre = "Requiere atencion";
        stage.orden = 1;
        stage.activo = true;
        stage.persist();

        ServiceType serviceType = new ServiceType();
        serviceType.nombre = "Tipo " + UUID.randomUUID();
        serviceType.persist();

        WorkOrder wo = new WorkOrder();
        wo.vehicle = vehicle;
        wo.stage = stage;
        wo.serviceType = serviceType;
        wo.prioridad = Prioridad.MEDIA;
        wo.descripcion = "Orden de prueba";
        wo.persist();
        return wo;
    }

    @Test
    @TestTransaction
    void registrar_persistsEntryWithoutAuthenticatedUser() {
        WorkOrder wo = createWorkOrder();

        service.registrar("WorkOrder", wo.id, "etapa", "Requiere atencion", "Programado");

        List<com.floatops.entity.AuditLog> logs =
                com.floatops.entity.AuditLog.buscar("WorkOrder", wo.id, null, null, null, 0, 20);

        assertEquals(1, logs.size());
        com.floatops.entity.AuditLog log = logs.get(0);
        assertEquals("WorkOrder", log.entidadAfectada);
        assertEquals(wo.id, log.entidadId);
        assertEquals("etapa", log.campo);
        assertEquals("Requiere atencion", log.valorAnterior);
        assertEquals("Programado", log.valorNuevo);
        // Sin @TestSecurity/JWT real en este test, no hay usuario autenticado.
        assertNull(log.userId);
    }

    @Test
    @TestTransaction
    void registrar_convertsNullValuesToNull() {
        WorkOrder wo = createWorkOrder();

        service.registrar("WorkOrder", wo.id, "creacion", null, "Orden creada");

        List<com.floatops.entity.AuditLog> logs =
                com.floatops.entity.AuditLog.buscar("WorkOrder", wo.id, null, null, null, 0, 20);

        assertEquals(1, logs.size());
        assertNull(logs.get(0).valorAnterior);
        assertEquals("Orden creada", logs.get(0).valorNuevo);
    }

    @Test
    @TestTransaction
    void listar_filtersByEntidadAfectadaAndEntidadId() {
        WorkOrder wo1 = createWorkOrder();
        WorkOrder wo2 = createWorkOrder();

        service.registrar("WorkOrder", wo1.id, "etapa", "A", "B");
        service.registrar("WorkOrder", wo2.id, "etapa", "A", "B");
        service.registrar("Vehicle", wo1.id, "estado", "ACTIVO", "DADO_DE_BAJA");

        ListAuditLogs200Response response = service.listar("WorkOrder", wo1.id, null, null, null, 0, 20);

        assertEquals(1, response.getTotal());
        assertEquals(1, response.getItems().size());
        AuditLog item = response.getItems().get(0);
        assertEquals("WorkOrder", item.getEntidadAfectada());
        assertEquals(wo1.id, item.getEntidadId());
    }

    @Test
    @TestTransaction
    void listar_filtersByDateRange() {
        WorkOrder wo = createWorkOrder();
        service.registrar("WorkOrder", wo.id, "etapa", "A", "B");

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfter = LocalDate.now().plusDays(2);

        ListAuditLogs200Response outsideRange = service.listar(null, null, null, tomorrow, dayAfter, 0, 20);
        assertEquals(0, outsideRange.getTotal());

        LocalDate yesterday = LocalDate.now().minusDays(1);
        ListAuditLogs200Response insideRange = service.listar(null, null, null, yesterday, tomorrow, 0, 20);
        assertTrue(insideRange.getTotal() >= 1);
    }

    @Test
    @TestTransaction
    void listar_paginatesResults() {
        WorkOrder wo = createWorkOrder();
        for (int i = 0; i < 5; i++) {
            service.registrar("WorkOrder", wo.id, "etapa", "A" + i, "B" + i);
        }

        ListAuditLogs200Response page0 = service.listar("WorkOrder", wo.id, null, null, null, 0, 2);
        assertEquals(2, page0.getItems().size());
        assertEquals(5, page0.getTotal());

        ListAuditLogs200Response page2 = service.listar("WorkOrder", wo.id, null, null, null, 2, 2);
        assertEquals(1, page2.getItems().size());
    }
}
