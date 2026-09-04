package com.floatops.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.floatops.entity.ServiceType;
import com.floatops.entity.Stage;
import com.floatops.entity.Technician;
import com.floatops.entity.Vehicle;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

/**
 * Pruebas de integracion HTTP contra WorkOrdersApi (generada desde
 * ApiContract/openapi-ordenes-trabajo.yaml). El foco es la matriz de
 * permisos de requerimientos-floatops.md seccion 2.1 (@RolesAllowed en
 * WorkOrderResource): que cada endpoint acepte el rol correcto y rechace
 * los demas, no repetir las reglas de negocio ya cubiertas en
 * WorkOrderServiceTest.
 *
 * @TestSecurity reemplaza la identidad de seguridad para el request, asi
 * que no hace falta un Keycloak real corriendo. Los datos de cada
 * escenario se insertan con QuarkusTransaction.requiringNew() porque
 * RestAssured hace una llamada HTTP real: el request corre en un hilo/
 * transaccion distinta a la del metodo de test, asi que necesitan quedar
 * confirmados (commit) para que el endpoint los vea.
 */
@QuarkusTest
class WorkOrderResourceTest {

    // Prefijo definido por RestResourceRoot.APPLICATION_PATH (generado del
    // OpenAPI: @ApplicationPath("/v1")).
    private static final String PATH = "/v1/work-orders";

    private static class Scenario {
        UUID workOrderId;
        UUID otherStageId;
        UUID technicianId;
        UUID itemId;
    }

    private Scenario createWorkOrder() {
        Scenario scenario = new Scenario();
        QuarkusTransaction.requiringNew().run(() -> {
            Vehicle vehicle = new Vehicle();
            vehicle.placa = "REST-" + UUID.randomUUID().toString().substring(0, 6);
            vehicle.persist();

            Stage stage = new Stage();
            stage.nombre = "Requiere atencion";
            stage.orden = 1;
            stage.activo = true;
            stage.persist();

            Stage otherStage = new Stage();
            otherStage.nombre = "Programado";
            otherStage.orden = 2;
            otherStage.activo = true;
            otherStage.persist();

            ServiceType serviceType = new ServiceType();
            serviceType.nombre = "Tipo REST " + UUID.randomUUID();
            serviceType.persist();

            Technician technician = new Technician();
            technician.nombre = "Tecnico REST " + UUID.randomUUID();
            technician.persist();

            com.floatops.entity.WorkOrder order = new com.floatops.entity.WorkOrder();
            order.vehicle = vehicle;
            order.stage = stage;
            order.serviceType = serviceType;
            order.prioridad = com.floatops.entity.Prioridad.MEDIA;
            order.descripcion = "Orden de prueba REST";
            order.presupuesto = new BigDecimal("100000");
            order.persist();

            com.floatops.entity.ChecklistItem item = new com.floatops.entity.ChecklistItem();
            item.workOrder = order;
            item.descripcion = "Revisar frenos";
            item.orden = 1;
            item.persist();

            scenario.workOrderId = order.id;
            scenario.otherStageId = otherStage.id;
            scenario.technicianId = technician.id;
            scenario.itemId = item.id;
        });
        return scenario;
    }

    private UUID createVehicleAndServiceType(String[] serviceTypeNameOut) {
        UUID[] vehicleIdHolder = new UUID[1];
        QuarkusTransaction.requiringNew().run(() -> {
            Vehicle vehicle = new Vehicle();
            vehicle.placa = "REST-" + UUID.randomUUID().toString().substring(0, 6);
            vehicle.persist();

            ServiceType serviceType = new ServiceType();
            serviceType.nombre = "Tipo REST " + UUID.randomUUID();
            serviceType.persist();

            vehicleIdHolder[0] = vehicle.id;
            serviceTypeNameOut[0] = serviceType.nombre;
        });
        return vehicleIdHolder[0];
    }

    // --- POST /work-orders (createWorkOrder) - RF-006/RF-007 ---

    @Test
    @TestSecurity(user = "dispatcher1", roles = "despachador")
    void createWorkOrder_allowsDispatcher() {
        String[] serviceTypeName = new String[1];
        UUID vehicleId = createVehicleAndServiceType(serviceTypeName);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"vehicleId":"%s","tipoServicio":"%s","descripcion":"Cambio de aceite","prioridad":"MEDIA"}
                        """.formatted(vehicleId, serviceTypeName[0]))
                .when().post(PATH)
                .then().statusCode(200)
                .body("vehicleId", equalTo(vehicleId.toString()));
    }

    @Test
    @TestSecurity(user = "technician1", roles = "tecnico")
    void createWorkOrder_rejectsTechnicianWith403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"vehicleId":"%s","tipoServicio":"x","descripcion":"x","prioridad":"MEDIA"}
                        """.formatted(UUID.randomUUID()))
                .when().post(PATH)
                .then().statusCode(403);
    }

    // --- GET /work-orders/{id} (getWorkOrder) - RF-011 ---

    @Test
    @TestSecurity(user = "technician1", roles = "tecnico")
    void getWorkOrder_allowsTechnician() {
        Scenario scenario = createWorkOrder();

        given()
                .when().get(PATH + "/" + scenario.workOrderId)
                .then().statusCode(200)
                .body("id", equalTo(scenario.workOrderId.toString()));
    }

    @Test
    void getWorkOrder_unauthenticatedReturns401() {
        Scenario scenario = createWorkOrder();

        given()
                .when().get(PATH + "/" + scenario.workOrderId)
                .then().statusCode(401);
    }

    // --- GET /work-orders (listWorkOrders) - RF-004/RF-037/RF-013 ---

    @Test
    @TestSecurity(user = "dispatcher1", roles = "despachador")
    void listWorkOrders_allowsDispatcher() {
        createWorkOrder();

        given()
                .when().get(PATH)
                .then().statusCode(200);
    }

    // --- PATCH /work-orders/{id}/stage (changeWorkOrderStage) - RF-005/RF-031 ---

    @Test
    @TestSecurity(user = "admin1", roles = "administrador")
    void changeWorkOrderStage_allowsAdministrator() {
        Scenario scenario = createWorkOrder();

        given()
                .contentType(ContentType.JSON)
                .body("{\"stageId\":\"" + scenario.otherStageId + "\"}")
                .when().patch(PATH + "/" + scenario.workOrderId + "/stage")
                .then().statusCode(200)
                .body("stageId", equalTo(scenario.otherStageId.toString()));
    }

    @Test
    @TestSecurity(user = "technician1", roles = "tecnico")
    void changeWorkOrderStage_rejectsTechnicianWith403() {
        Scenario scenario = createWorkOrder();

        given()
                .contentType(ContentType.JSON)
                .body("{\"stageId\":\"" + scenario.otherStageId + "\"}")
                .when().patch(PATH + "/" + scenario.workOrderId + "/stage")
                .then().statusCode(403);
    }

    // --- PUT /work-orders/{id}/technician (assignTechnician) - RF-008/RF-032 ---

    @Test
    @TestSecurity(user = "dispatcher1", roles = "despachador")
    void assignTechnician_allowsDispatcher() {
        Scenario scenario = createWorkOrder();

        given()
                .contentType(ContentType.JSON)
                .body("{\"technicianId\":\"" + scenario.technicianId + "\"}")
                .when().put(PATH + "/" + scenario.workOrderId + "/technician")
                .then().statusCode(200)
                .body("technicianId", equalTo(scenario.technicianId.toString()));
    }

    @Test
    @TestSecurity(user = "technician1", roles = "tecnico")
    void assignTechnician_rejectsTechnicianWith403() {
        Scenario scenario = createWorkOrder();

        given()
                .contentType(ContentType.JSON)
                .body("{\"technicianId\":\"" + scenario.technicianId + "\"}")
                .when().put(PATH + "/" + scenario.workOrderId + "/technician")
                .then().statusCode(403);
    }

    // --- POST /work-orders/{id}/notes (addNote) - RF-014 ---

    @Test
    @TestSecurity(user = "technician1", roles = "tecnico")
    void addNote_allowsTechnician() {
        Scenario scenario = createWorkOrder();

        given()
                .contentType(ContentType.JSON)
                .body("{\"texto\":\"Nota de prueba\"}")
                .when().post(PATH + "/" + scenario.workOrderId + "/notes")
                .then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "dispatcher1", roles = "despachador")
    void addNote_rejectsDispatcherWith403() {
        Scenario scenario = createWorkOrder();

        given()
                .contentType(ContentType.JSON)
                .body("{\"texto\":\"Nota de prueba\"}")
                .when().post(PATH + "/" + scenario.workOrderId + "/notes")
                .then().statusCode(403);
    }

    // --- POST /work-orders/{id}/photos (attachPhoto) - RF-009/RF-014 ---

    @Test
    @TestSecurity(user = "technician1", roles = "tecnico")
    void attachPhoto_allowsTechnician() {
        Scenario scenario = createWorkOrder();

        given()
                .multiPart("file", "foto.jpg", "contenido de prueba".getBytes())
                .when().post(PATH + "/" + scenario.workOrderId + "/photos")
                .then().statusCode(200)
                .body("url", org.hamcrest.Matchers.startsWith(scenario.workOrderId.toString()));
    }

    // --- POST /work-orders/{id}/ready-for-review (markReadyForReview) - RF-015 ---

    @Test
    @TestSecurity(user = "technician1", roles = "tecnico")
    void markReadyForReview_allowsTechnician() {
        Scenario scenario = createWorkOrder();

        given()
                .when().post(PATH + "/" + scenario.workOrderId + "/ready-for-review")
                .then().statusCode(200)
                .body("listaParaRevision", equalTo(true));
    }

    @Test
    @TestSecurity(user = "dispatcher1", roles = "despachador")
    void markReadyForReview_rejectsDispatcherWith403() {
        Scenario scenario = createWorkOrder();

        given()
                .when().post(PATH + "/" + scenario.workOrderId + "/ready-for-review")
                .then().statusCode(403);
    }

    // --- POST /work-orders/{id}/quote-approval (registerClientQuoteApproval) - RF-033 ---

    @Test
    @TestSecurity(user = "dispatcher1", roles = "despachador")
    void registerClientQuoteApproval_allowsDispatcher() {
        Scenario scenario = createWorkOrder();

        given()
                .when().post(PATH + "/" + scenario.workOrderId + "/quote-approval")
                .then().statusCode(200)
                .body("cotizacionAprobadaCliente", equalTo(true));
    }

    // --- POST /work-orders/{id}/supervisor-approval (registerSupervisorQuoteApproval) - RF-039 ---

    @Test
    @TestSecurity(user = "supervisor1", roles = "supervisor-gerente")
    void registerSupervisorQuoteApproval_allowsSupervisor() {
        Scenario scenario = createWorkOrder();

        given()
                .when().post(PATH + "/" + scenario.workOrderId + "/supervisor-approval")
                .then().statusCode(200)
                .body("cotizacionAprobadaSupervisor", equalTo(true));
    }

    @Test
    @TestSecurity(user = "dispatcher1", roles = "despachador")
    void registerSupervisorQuoteApproval_rejectsDispatcherWith403() {
        Scenario scenario = createWorkOrder();

        given()
                .when().post(PATH + "/" + scenario.workOrderId + "/supervisor-approval")
                .then().statusCode(403);
    }

    // --- PATCH /work-orders/{id}/checklist/{itemId} (updateChecklistItem) - RF-012 ---

    @Test
    @TestSecurity(user = "dispatcher1", roles = "despachador")
    void updateChecklistItem_allowsDispatcher() {
        Scenario scenario = createWorkOrder();

        given()
                .contentType(ContentType.JSON)
                .body("{\"completado\":true}")
                .when().patch(PATH + "/" + scenario.workOrderId + "/checklist/" + scenario.itemId)
                .then().statusCode(200)
                .body("completado", equalTo(true));
    }
}
