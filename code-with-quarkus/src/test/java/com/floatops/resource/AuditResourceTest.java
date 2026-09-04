package com.floatops.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.floatops.entity.Prioridad;
import com.floatops.entity.ServiceType;
import com.floatops.entity.Stage;
import com.floatops.entity.Vehicle;
import com.floatops.entity.WorkOrder;
import com.floatops.service.AuditService;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/**
 * Pruebas de integracion HTTP contra AuditLogsApi (generada desde
 * ApiContract/openapi-auditoria.yaml). Solo existe el endpoint de
 * consulta - el registro es interno (ver AuditServiceTest). El foco es
 * la matriz de permisos: unico rol autorizado es Administrador.
 */
@QuarkusTest
class AuditResourceTest {

    private static final String PATH = "/v1/audit-logs";

    @Inject
    AuditService auditService;

    private UUID createAuditedWorkOrder() {
        UUID[] workOrderId = new UUID[1];
        QuarkusTransaction.requiringNew().run(() -> {
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
            wo.descripcion = "Orden de prueba REST";
            wo.persist();

            workOrderId[0] = wo.id;
        });
        QuarkusTransaction.requiringNew()
                .run(() -> auditService.registrar("WorkOrder", workOrderId[0], "etapa", "A", "B"));
        return workOrderId[0];
    }

    @Test
    @TestSecurity(user = "admin1", roles = "administrador")
    void listAuditLogs_allowsAdministrator() {
        UUID workOrderId = createAuditedWorkOrder();

        given()
                .queryParam("entidadAfectada", "WorkOrder")
                .queryParam("entidadId", workOrderId.toString())
                .when().get(PATH)
                .then().statusCode(200)
                .body("total", equalTo(1))
                .body("items[0].entidadId", equalTo(workOrderId.toString()));
    }

    @Test
    @TestSecurity(user = "dispatcher1", roles = "despachador")
    void listAuditLogs_rejectsDispatcherWith403() {
        given()
                .when().get(PATH)
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "technician1", roles = "tecnico")
    void listAuditLogs_rejectsTechnicianWith403() {
        given()
                .when().get(PATH)
                .then().statusCode(403);
    }

    @Test
    void listAuditLogs_unauthenticatedReturns401() {
        given()
                .when().get(PATH)
                .then().statusCode(401);
    }
}
