package com.floatops.service;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.floatops.dto.generated.WorkOrder;
import com.floatops.dto.generated.WorkOrderCreate;
import com.floatops.entity.Prioridad;
import com.floatops.entity.ServiceType;
import com.floatops.entity.Stage;
import com.floatops.entity.Vehicle;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

/**
 * NOTA sobre el cambio de estrategia: el intento original con
 * Mockito.mockStatic(WorkOrder.class) fallaba porque findById() esta
 * declarado en PanacheEntityBase (la superclase), no en WorkOrder.class
 * directamente - mockStatic() solo intercepta metodos declarados en la
 * clase exacta que le pasas, asi que nunca capturaba la llamada real.
 * Es un choque conocido entre el mock estatico de Mockito y como
 * Quarkus genera el bytecode de las entidades Panache.
 *
 * Por eso esto es @QuarkusTest con base de datos real (Dev Services,
 * ver application.properties) en vez de mocks. @TestTransaction hace
 * rollback automatico despues de cada test, asi que no hace falta
 * limpieza manual ni los tests se contaminan entre si.
 */
@QuarkusTest
class WorkOrderServiceTest {

    @Inject
    WorkOrderService service;

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("2000000");

    private Vehicle createVehicle() {
        Vehicle v = new Vehicle();
        v.placa = "TEST-" + UUID.randomUUID().toString().substring(0, 6);
        v.persist();
        return v;
    }

    private Stage createStage(String name, int order) {
        Stage s = new Stage();
        s.nombre = name;
        s.orden = order;
        s.activo = true;
        s.persist();
        return s;
    }

    private ServiceType createServiceType() {
        ServiceType st = new ServiceType();
        st.nombre = "Tipo de prueba " + UUID.randomUUID();
        st.persist();
        return st;
    }

    private com.floatops.entity.WorkOrder createWorkOrder(Vehicle vehicle, Stage stage, ServiceType serviceType,
                                                            BigDecimal budget, Boolean supervisorApproved) {
        com.floatops.entity.WorkOrder order = new com.floatops.entity.WorkOrder();
        order.vehicle = vehicle;
        order.stage = stage;
        order.serviceType = serviceType;
        order.prioridad = Prioridad.MEDIA;
        order.descripcion = "Orden de prueba";
        order.presupuesto = budget;
        order.cotizacionAprobadaSupervisor = supervisorApproved;
        order.persist();
        return order;
    }

    private com.floatops.entity.Technician createTechnician() {
        com.floatops.entity.Technician t = new com.floatops.entity.Technician();
        t.nombre = "Tecnico de prueba " + UUID.randomUUID();
        t.persist();
        return t;
    }

    private com.floatops.entity.Part createPart(BigDecimal price) {
        com.floatops.entity.Part p = new com.floatops.entity.Part();
        p.codigo = "PART-" + UUID.randomUUID().toString().substring(0, 8);
        p.nombre = "Repuesto de prueba";
        p.precio = price;
        p.persist();
        return p;
    }

    // --- crear() ---

    @Test
    @TestTransaction
    void create_throws400_whenVehicleDoesNotExist() {
        WorkOrderCreate dto = new WorkOrderCreate();
        dto.setVehicleId(UUID.randomUUID()); // no existe
        dto.setTipoServicio("Cualquiera");
        dto.setDescripcion("test");
        dto.setPrioridad(com.floatops.dto.generated.Prioridad.MEDIA);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.crear(dto));

        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void create_throws400_whenServiceTypeDoesNotExist() {
        Vehicle vehicle = createVehicle();
        createStage("Requiere atencion", 1);

        WorkOrderCreate dto = new WorkOrderCreate();
        dto.setVehicleId(vehicle.id);
        dto.setTipoServicio("No existe");
        dto.setDescripcion("test");
        dto.setPrioridad(com.floatops.dto.generated.Prioridad.MEDIA);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.crear(dto));

        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void create_throws400_whenTechnicianDoesNotExist() {
        Vehicle vehicle = createVehicle();
        createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();

        WorkOrderCreate dto = new WorkOrderCreate();
        dto.setVehicleId(vehicle.id);
        dto.setTipoServicio(serviceType.nombre);
        dto.setDescripcion("test");
        dto.setPrioridad(com.floatops.dto.generated.Prioridad.MEDIA);
        dto.setTechnicianId(UUID.randomUUID()); // no existe

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.crear(dto));

        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void create_throws400_whenPartDoesNotExist() {
        Vehicle vehicle = createVehicle();
        createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();

        WorkOrderCreate dto = new WorkOrderCreate();
        dto.setVehicleId(vehicle.id);
        dto.setTipoServicio(serviceType.nombre);
        dto.setDescripcion("test");
        dto.setPrioridad(com.floatops.dto.generated.Prioridad.MEDIA);
        dto.setPartIds(java.util.List.of(UUID.randomUUID())); // no existe

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.crear(dto));

        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void create_createsFullOrder_withTechnicianAndParts_calculatesBudget() {
        Vehicle vehicle = createVehicle();
        createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.Technician technician = createTechnician();
        com.floatops.entity.Part partA = createPart(new BigDecimal("100000"));
        com.floatops.entity.Part partB = createPart(new BigDecimal("50000"));

        WorkOrderCreate dto = new WorkOrderCreate();
        dto.setVehicleId(vehicle.id);
        dto.setTipoServicio(serviceType.nombre);
        dto.setDescripcion("Cambio de frenos");
        dto.setPrioridad(com.floatops.dto.generated.Prioridad.ALTA);
        dto.setTechnicianId(technician.id);
        dto.setPartIds(java.util.List.of(partA.id, partB.id));

        WorkOrder result = service.crear(dto);

        assertEquals(vehicle.id, result.getVehicleId());
        assertEquals(technician.id, result.getTechnicianId());
        assertEquals(0, new BigDecimal("150000").compareTo(
                BigDecimal.valueOf(result.getPresupuesto())));
    }

    // --- cambiarEtapa(): bloqueo por cotizacion de alto valor (RF-033/RF-039) ---

    @Test
    @TestTransaction
    void changeStage_blocksWith409_whenHighValueAndNotApprovedAndEnteringWorkshop() {
        Vehicle vehicle = createVehicle();
        Stage attentionRequiredStage = createStage("Requiere atencion", 1);
        Stage workshopStage = createStage("En taller", 2);
        ServiceType serviceType = createServiceType();

        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, attentionRequiredStage, serviceType,
                HIGH_VALUE_THRESHOLD.add(BigDecimal.ONE), null);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.cambiarEtapa(order.id, workshopStage.id));

        assertEquals(409, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void changeStage_allows_whenHighValueButAlreadyApprovedBySupervisor() {
        Vehicle vehicle = createVehicle();
        Stage attentionRequiredStage = createStage("Requiere atencion", 1);
        Stage workshopStage = createStage("En taller", 2);
        ServiceType serviceType = createServiceType();

        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, attentionRequiredStage, serviceType,
                HIGH_VALUE_THRESHOLD.add(BigDecimal.ONE), true);

        WorkOrder result = service.cambiarEtapa(order.id, workshopStage.id);

        assertEquals(workshopStage.id, result.getStageId());
    }

    @Test
    @TestTransaction
    void changeStage_allows_whenAmountBelowThreshold_evenIfNotApproved() {
        Vehicle vehicle = createVehicle();
        Stage attentionRequiredStage = createStage("Requiere atencion", 1);
        Stage workshopStage = createStage("En taller", 2);
        ServiceType serviceType = createServiceType();

        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, attentionRequiredStage, serviceType,
                new BigDecimal("500000"), null);

        WorkOrder result = service.cambiarEtapa(order.id, workshopStage.id);

        assertEquals(workshopStage.id, result.getStageId());
    }

    @Test
    @TestTransaction
    void changeStage_allows_whenHighValueButStageIsNotWorkshop() {
        Vehicle vehicle = createVehicle();
        Stage attentionRequiredStage = createStage("Requiere atencion", 1);
        Stage scheduledStage = createStage("Programado", 2);
        ServiceType serviceType = createServiceType();

        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, attentionRequiredStage, serviceType,
                HIGH_VALUE_THRESHOLD.add(BigDecimal.ONE), null);

        WorkOrder result = service.cambiarEtapa(order.id, scheduledStage.id);

        assertEquals(scheduledStage.id, result.getStageId());
    }

    @Test
    @TestTransaction
    void changeStage_throws404_whenOrderDoesNotExist() {
        Stage stage = createStage("Programado", 1);

        assertThrows(NotFoundException.class,
                () -> service.cambiarEtapa(UUID.randomUUID(), stage.id));
    }

    @Test
    @TestTransaction
    void changeStage_throws400_whenStageDoesNotExist() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Programado", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.cambiarEtapa(order.id, UUID.randomUUID()));

        assertEquals(400, exception.getResponse().getStatus());
    }

    // --- obtener() ---

    @Test
    @TestTransaction
    void get_throws404_whenOrderDoesNotExist() {
        assertThrows(NotFoundException.class, () -> service.obtener(UUID.randomUUID()));
    }

    @Test
    @TestTransaction
    void get_returnsDTO_whenOrderExists() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);

        WorkOrder result = service.obtener(order.id);

        assertEquals(order.id, result.getId());
        assertEquals(order.descripcion, result.getDescripcion());
    }

    // --- marcarListaParaRevision() (RF-015) ---

    @Test
    @TestTransaction
    void markReadyForReview_updatesField() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);

        WorkOrder result = service.marcarListaParaRevision(order.id);

        assertTrue(result.getListaParaRevision());
    }

    // --- listar() ---

    @Test
    @TestTransaction
    void list_filtersByStage() {
        Vehicle vehicle = createVehicle();
        Stage stageA = createStage("Etapa A", 1);
        Stage stageB = createStage("Etapa B", 2);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder orderInA = createWorkOrder(vehicle, stageA, serviceType, null, null);
        createWorkOrder(vehicle, stageB, serviceType, null, null);

        com.floatops.dto.generated.ListWorkOrders200Response result =
                service.listar(stageA.id, null, null, null, null);

        assertEquals(1, result.getTotal());
        assertEquals(orderInA.id, result.getItems().get(0).getId());
    }

    @Test
    @TestTransaction
    void list_filtersByTechnician() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Etapa A", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.Technician technician = createTechnician();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);
        order.technician = technician;
        createWorkOrder(vehicle, stage, serviceType, null, null); // sin tecnico, no deberia salir

        com.floatops.dto.generated.ListWorkOrders200Response result =
                service.listar(null, technician.id.toString(), null, null, null);

        assertEquals(1, result.getTotal());
        assertEquals(order.id, result.getItems().get(0).getId());
    }

    @Test
    @TestTransaction
    void list_filtersByOpenStatus_excludesCompleted() {
        Vehicle vehicle = createVehicle();
        Stage completedStage = createStage("Completados", 1);
        Stage workshopStage = createStage("En taller", 2);
        ServiceType serviceType = createServiceType();
        createWorkOrder(vehicle, completedStage, serviceType, null, null);
        com.floatops.entity.WorkOrder openOrder = createWorkOrder(vehicle, workshopStage, serviceType, null, null);

        com.floatops.dto.generated.ListWorkOrders200Response result =
                service.listar(null, null, "open", null, null);

        boolean includesOpenOrder = result.getItems().stream().anyMatch(i -> openOrder.id.equals(i.getId()));
        boolean includesCompletedOrder = result.getItems().stream()
                .anyMatch(i -> "Completados".equals(i.getStageName()));
        assertTrue(includesOpenOrder);
        assertTrue(!includesCompletedOrder);
    }

    @Test
    @TestTransaction
    void list_withoutFilters_returnsAll() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Etapa A", 1);
        ServiceType serviceType = createServiceType();
        createWorkOrder(vehicle, stage, serviceType, null, null);
        createWorkOrder(vehicle, stage, serviceType, null, null);

        com.floatops.dto.generated.ListWorkOrders200Response result =
                service.listar(null, null, null, null, null);

        assertTrue(result.getTotal() >= 2);
    }

    // --- asignarTecnico() (RF-008/RF-032) ---

    @Test
    @TestTransaction
    void assignTechnician_updatesTechnician() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);
        com.floatops.entity.Technician technician = createTechnician();

        WorkOrder result = service.asignarTecnico(order.id, technician.id);

        assertEquals(technician.id, result.getTechnicianId());
    }

    @Test
    @TestTransaction
    void assignTechnician_throws404_whenOrderDoesNotExist() {
        com.floatops.entity.Technician technician = createTechnician();

        assertThrows(NotFoundException.class,
                () -> service.asignarTecnico(UUID.randomUUID(), technician.id));
    }

    @Test
    @TestTransaction
    void assignTechnician_throws400_whenTechnicianDoesNotExist() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.asignarTecnico(order.id, UUID.randomUUID()));

        assertEquals(400, exception.getResponse().getStatus());
    }

    // --- adjuntarFoto() (RF-009/RF-014) ---

    @Test
    @TestTransaction
    void attachPhoto_uploadsFileAndRegistersPhoto() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);

        com.floatops.dto.generated.Photo result = service.adjuntarFoto(order.id,
                new java.io.ByteArrayInputStream("contenido de prueba".getBytes()));

        com.floatops.entity.WorkOrder persisted = com.floatops.entity.WorkOrder.findById(order.id);
        assertEquals(1, persisted.photos.size());
        assertTrue(result.getUrl().startsWith(order.id.toString()));
    }

    @Test
    @TestTransaction
    void attachPhoto_throws404_whenOrderDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> service.adjuntarFoto(UUID.randomUUID(),
                        new java.io.ByteArrayInputStream("x".getBytes())));
    }

    // --- agregarNota() (RF-014) ---

    @Test
    @TestTransaction
    void addNote_addsNoteToOrder() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);

        service.agregarNota(order.id, "Nota de prueba");

        WorkOrder result = service.obtener(order.id);
        assertEquals(1, result.getNotes().size());
        assertEquals("Nota de prueba", result.getNotes().get(0).getTexto());
    }

    @Test
    @TestTransaction
    void addNote_throws404_whenOrderDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> service.agregarNota(UUID.randomUUID(), "texto"));
    }

    // --- aprobarCotizacionCliente() (RF-033) ---

    @Test
    @TestTransaction
    void approveClientQuote_marksApproved() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);

        WorkOrder result = service.aprobarCotizacionCliente(order.id);

        assertTrue(result.getCotizacionAprobadaCliente());
    }

    @Test
    @TestTransaction
    void approveClientQuote_throws404_whenOrderDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> service.aprobarCotizacionCliente(UUID.randomUUID()));
    }

    // --- aprobarCotizacionSupervisor() (RF-039) ---

    @Test
    @TestTransaction
    void approveSupervisorQuote_marksApproved() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType,
                HIGH_VALUE_THRESHOLD.add(BigDecimal.ONE), null);

        WorkOrder result = service.aprobarCotizacionSupervisor(order.id);

        assertTrue(result.getCotizacionAprobadaSupervisor());
    }

    @Test
    @TestTransaction
    void approveSupervisorQuote_throws404_whenOrderDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> service.aprobarCotizacionSupervisor(UUID.randomUUID()));
    }

    // --- actualizarChecklistItem() (RF-012) ---

    private com.floatops.entity.ChecklistItem createChecklistItem(com.floatops.entity.WorkOrder order) {
        com.floatops.entity.ChecklistItem item = new com.floatops.entity.ChecklistItem();
        item.workOrder = order;
        item.descripcion = "Revisar frenos";
        item.orden = 1;
        item.persist();
        return item;
    }

    @Test
    @TestTransaction
    void updateChecklistItem_marksCompleted() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);
        com.floatops.entity.ChecklistItem item = createChecklistItem(order);

        com.floatops.dto.generated.ChecklistItem result =
                service.actualizarChecklistItem(order.id, item.id, true);

        assertTrue(result.getCompletado());
    }

    @Test
    @TestTransaction
    void updateChecklistItem_throws404_whenItemDoesNotExist() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder order = createWorkOrder(vehicle, stage, serviceType, null, null);

        assertThrows(NotFoundException.class,
                () -> service.actualizarChecklistItem(order.id, UUID.randomUUID(), true));
    }

    @Test
    @TestTransaction
    void updateChecklistItem_throws404_whenItemBelongsToAnotherOrder() {
        Vehicle vehicle = createVehicle();
        Stage stage = createStage("Requiere atencion", 1);
        ServiceType serviceType = createServiceType();
        com.floatops.entity.WorkOrder orderA = createWorkOrder(vehicle, stage, serviceType, null, null);
        com.floatops.entity.WorkOrder orderB = createWorkOrder(vehicle, stage, serviceType, null, null);
        com.floatops.entity.ChecklistItem item = createChecklistItem(orderA);

        assertThrows(NotFoundException.class,
                () -> service.actualizarChecklistItem(orderB.id, item.id, true));
    }

    // --- crear(): sin etapas configuradas (caso limite de RF-006/RF-007) ---

    @Test
    @TestTransaction
    void create_throws500_whenNoActiveStagesConfigured() {
        // Desactiva (dentro de esta transaccion, que se revierte al final
        // del test) cualquier etapa que hubieran dejado otras pruebas, para
        // simular el catalogo sin etapas activas. No se borran las filas
        // porque otras pruebas (fuera de esta transaccion, ya confirmadas)
        // pueden tener ordenes que las referencian por llave foranea.
        Stage.update("activo = false");

        Vehicle vehicle = createVehicle();
        ServiceType serviceType = createServiceType();

        WorkOrderCreate dto = new WorkOrderCreate();
        dto.setVehicleId(vehicle.id);
        dto.setTipoServicio(serviceType.nombre);
        dto.setDescripcion("test");
        dto.setPrioridad(com.floatops.dto.generated.Prioridad.MEDIA);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.crear(dto));

        assertEquals(500, exception.getResponse().getStatus());
    }
}
