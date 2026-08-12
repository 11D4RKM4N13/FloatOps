package com.floatops.resource;

import com.floatops.dto.generated.*;
import com.floatops.resource.generated.WorkOrdersApi;
import com.floatops.service.WorkOrderService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.util.UUID;

// Implementa la interfaz generada por openapi-generator desde
// ApiContract/openapi-ordenes-trabajo.yaml. Las anotaciones JAX-RS
// (@Path, @GET, etc.) viven en WorkOrdersApi y se heredan aqui - no
// hace falta repetirlas en esta clase.
//
// @RolesAllowed en cada metodo replica exactamente la matriz de permisos
// de requerimientos-floatops.md seccion 2.1 - no es una decision nueva,
// es la misma ya tomada, ahora aplicada en codigo.
public class WorkOrderResource implements WorkOrdersApi {

    @Inject
    WorkOrderService service;

    @Override
    @RolesAllowed({"tecnico"}) // RF-014: actor unico es Tecnico
    public void addNote(UUID workOrderId, AddNoteRequest addNoteRequest) {
        // ASUMIDO: AddNoteRequest.getTexto() segun el schema original
        // (required: [texto]). Verificar si no compila.
        service.agregarNota(workOrderId, addNoteRequest.getTexto());
    }

    @Override
    @RolesAllowed({"despachador", "administrador"}) // RF-008/RF-032
    public WorkOrder assignTechnician(UUID workOrderId, AssignTechnicianRequest assignTechnicianRequest) {
        // ASUMIDO: AssignTechnicianRequest.getTechnicianId() : UUID
        return service.asignarTecnico(workOrderId, assignTechnicianRequest.getTechnicianId());
    }

    @Override
    @RolesAllowed({"despachador", "tecnico"}) // RF-009 (despachador) / RF-014 (tecnico)
    public Photo attachPhoto(UUID workOrderId, InputStream fileInputStream) {
        return service.adjuntarFoto(workOrderId, fileInputStream);
    }

    @Override
    @RolesAllowed({"despachador", "administrador"}) // RF-005/RF-031 - explicito: Tecnico NO
    public WorkOrder changeWorkOrderStage(UUID workOrderId, ChangeWorkOrderStageRequest changeWorkOrderStageRequest) {
        // ASUMIDO: ChangeWorkOrderStageRequest.getStageId() : UUID
        return service.cambiarEtapa(workOrderId, changeWorkOrderStageRequest.getStageId());
    }

    @Override
    @RolesAllowed({"despachador", "administrador"}) // RF-006/RF-007
    public WorkOrder createWorkOrder(WorkOrderCreate workOrderCreate) {
        return service.crear(workOrderCreate);
    }

    @Override
    @RolesAllowed({"despachador", "administrador", "tecnico"}) // RF-011
    public WorkOrder getWorkOrder(UUID workOrderId) {
        return service.obtener(workOrderId);
    }

    @Override
    @RolesAllowed({"despachador", "administrador", "tecnico"}) // RF-004/RF-037/RF-013
    public ListWorkOrders200Response listWorkOrders(UUID stageId, String technicianId, String status, Integer page, Integer size) {
        return service.listar(stageId, technicianId, status, page, size);
    }

    @Override
    @RolesAllowed({"tecnico"}) // RF-015
    public WorkOrder markReadyForReview(UUID workOrderId) {
        return service.marcarListaParaRevision(workOrderId);
    }

    @Override
    @RolesAllowed({"despachador", "administrador"}) // RF-033
    public WorkOrder registerClientQuoteApproval(UUID workOrderId) {
        return service.aprobarCotizacionCliente(workOrderId);
    }

    @Override
    @RolesAllowed({"supervisor-gerente"}) // RF-039 - unico rol autorizado
    public WorkOrder registerSupervisorQuoteApproval(UUID workOrderId) {
        return service.aprobarCotizacionSupervisor(workOrderId);
    }

    @Override
    @RolesAllowed({"despachador", "tecnico"}) // RF-012
    public ChecklistItem updateChecklistItem(UUID workOrderId, UUID itemId, UpdateChecklistItemRequest updateChecklistItemRequest) {
        // ASUMIDO: UpdateChecklistItemRequest.getCompletado() : Boolean
        return service.actualizarChecklistItem(workOrderId, itemId, updateChecklistItemRequest.getCompletado());
    }
}