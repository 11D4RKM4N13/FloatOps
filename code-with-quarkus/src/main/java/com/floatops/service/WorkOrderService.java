package com.floatops.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.floatops.dto.generated.ListWorkOrders200Response;
import com.floatops.dto.generated.Photo;
import com.floatops.dto.generated.WorkOrder;
import com.floatops.dto.generated.WorkOrderCreate;
import com.floatops.dto.generated.WorkOrderSummary;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class WorkOrderService {

    // RF-040: deberia venir de /settings/quote-approval-threshold cuando
    // exista el modulo de Administracion. Hardcodeado hasta entonces.
    private static final BigDecimal UMBRAL_ALTO_VALOR = new BigDecimal("2000000");

    @Inject
    AuditService auditService;

    @Transactional
    public WorkOrder crear(WorkOrderCreate dto) {
        com.floatops.entity.Vehicle vehicle = com.floatops.entity.Vehicle.findById(dto.getVehicleId());
        if (vehicle == null) {
            throw new WebApplicationException("Vehiculo no encontrado", Response.Status.BAD_REQUEST);
        }

        // NOTA: tipoServicio en el contrato es un String (nombre del tipo de
        // servicio), pero ServiceType es una entidad con id UUID. Buscamos
        // por nombre - esto asume que el catalogo se consulta por nombre
        // desde el frontend, no por id. Revisar si prefieres que el
        // contrato mande serviceTypeId (UUID) en vez de un String libre.
        com.floatops.entity.ServiceType serviceType =
                com.floatops.entity.ServiceType.find("nombre", dto.getTipoServicio()).firstResult();
        if (serviceType == null) {
            throw new WebApplicationException("Tipo de servicio no encontrado", Response.Status.BAD_REQUEST);
        }

        com.floatops.entity.WorkOrder wo = new com.floatops.entity.WorkOrder();
        wo.vehicle = vehicle;
        wo.serviceType = serviceType;
        wo.descripcion = dto.getDescripcion();
        wo.prioridad = com.floatops.entity.Prioridad.valueOf(dto.getPrioridad().name());
        wo.fechaProgramada = dto.getFechaProgramada();

        if (dto.getTechnicianId() != null) {
            com.floatops.entity.Technician technician =
                    com.floatops.entity.Technician.findById(dto.getTechnicianId());
            if (technician == null) {
                throw new WebApplicationException("Tecnico no encontrado", Response.Status.BAD_REQUEST);
            }
            wo.technician = technician;
        }

        wo.stage = com.floatops.entity.Stage.findAllActiveOrdered().stream().findFirst()
                .orElseThrow(() -> new WebApplicationException(
                        "No hay etapas configuradas", Response.Status.INTERNAL_SERVER_ERROR));

        if (dto.getPartIds() != null && !dto.getPartIds().isEmpty()) {
            wo.presupuesto = BigDecimal.ZERO;
            for (UUID partId : dto.getPartIds()) {
                com.floatops.entity.Part part = com.floatops.entity.Part.findById(partId);
                if (part == null) {
                    throw new WebApplicationException(
                            "Repuesto no encontrado: " + partId, Response.Status.BAD_REQUEST);
                }
                wo.presupuesto = wo.presupuesto.add(part.precio);

                com.floatops.entity.WorkOrderPart wop = new com.floatops.entity.WorkOrderPart();
                wop.workOrder = wo;
                wop.part = part;
                wo.parts.add(wop);
            }
        }

        wo.persist();

        // TODO (RF-026): NotificationService.enviar(...)
        auditService.registrar("WorkOrder", wo.id, "creacion", null, "Orden creada");

        return toApiWorkOrder(wo);
    }

    public ListWorkOrders200Response listar(UUID stageId, String technicianId, String status, Integer page, Integer size) {
        List<com.floatops.entity.WorkOrder> ordenes;
        if (stageId != null) {
            ordenes = com.floatops.entity.WorkOrder.findByStage(stageId);
        } else if (technicianId != null && !"me".equals(technicianId)) {
            // TODO: soportar "me" cuando exista OIDC y sepamos el usuario autenticado
            ordenes = com.floatops.entity.WorkOrder.findByTechnician(UUID.fromString(technicianId));
        } else if ("open".equals(status)) {
            ordenes = com.floatops.entity.WorkOrder.findOpen();
        } else {
            ordenes = com.floatops.entity.WorkOrder.listAll();
        }

        List<WorkOrderSummary> items = ordenes.stream()
                .map(this::toApiSummary)
                .collect(Collectors.toList());

        // ASUMIDO: ListWorkOrders200Response.items(List) / .total(Integer)
        // segun el schema original (items, total). Verificar si no compila.
        ListWorkOrders200Response response = new ListWorkOrders200Response();
        response.setItems(items);
        response.setTotal(items.size());
        return response;
    }

    public WorkOrder obtener(UUID id) {
        com.floatops.entity.WorkOrder wo = com.floatops.entity.WorkOrder.findById(id);
        if (wo == null) {
            throw new NotFoundException("Orden no encontrada");
        }
        return toApiWorkOrder(wo);
    }

    @Transactional
    public WorkOrder cambiarEtapa(UUID id, UUID nuevaEtapaId) {
        com.floatops.entity.WorkOrder wo = com.floatops.entity.WorkOrder.findById(id);
        if (wo == null) {
            throw new NotFoundException("Orden no encontrada");
        }

        com.floatops.entity.Stage nuevaEtapa = com.floatops.entity.Stage.findById(nuevaEtapaId);
        if (nuevaEtapa == null) {
            throw new WebApplicationException("Etapa no encontrada", Response.Status.BAD_REQUEST);
        }

        boolean esAltoValor = wo.presupuesto != null
                && wo.presupuesto.compareTo(UMBRAL_ALTO_VALOR) > 0;
        boolean entrandoATaller = "En taller".equalsIgnoreCase(nuevaEtapa.nombre);
        if (entrandoATaller && esAltoValor && !Boolean.TRUE.equals(wo.cotizacionAprobadaSupervisor)) {
            throw new WebApplicationException(
                    "Cotizacion de alto valor requiere aprobacion de Supervisor/Gerente antes de En taller",
                    Response.Status.CONFLICT);
        }

        String etapaAnterior = wo.stage.nombre;
        wo.stage = nuevaEtapa;

        // TODO (RF-026): NotificationService.enviar(...)
        auditService.registrar("WorkOrder", wo.id, "etapa", etapaAnterior, nuevaEtapa.nombre);

        return toApiWorkOrder(wo);
    }

    @Transactional
    public WorkOrder asignarTecnico(UUID id, UUID technicianId) {
        com.floatops.entity.WorkOrder wo = com.floatops.entity.WorkOrder.findById(id);
        if (wo == null) {
            throw new NotFoundException("Orden no encontrada");
        }
        com.floatops.entity.Technician technician = com.floatops.entity.Technician.findById(technicianId);
        if (technician == null) {
            throw new WebApplicationException("Tecnico no encontrado", Response.Status.BAD_REQUEST);
        }
        String tecnicoAnterior = wo.technician != null ? wo.technician.nombre : "Sin asignar";
        wo.technician = technician;

        // TODO (RF-026): notificacion de la reasignacion
        auditService.registrar("WorkOrder", wo.id, "tecnico", tecnicoAnterior, technician.nombre);

        return toApiWorkOrder(wo);
    }

    @Inject
    software.amazon.awssdk.services.s3.S3Client s3Client;

    @Transactional
    public Photo adjuntarFoto(UUID workOrderId, InputStream fileInputStream) {
        com.floatops.entity.WorkOrder wo = com.floatops.entity.WorkOrder.findById(workOrderId);
        if (wo == null) {
            throw new NotFoundException("Orden no encontrada");
        }

        byte[] bytes;
        try {
            bytes = fileInputStream.readAllBytes();
        } catch (java.io.IOException e) {
            throw new WebApplicationException("Error leyendo el archivo", Response.Status.BAD_REQUEST);
        }

        // NOTA: se usa la sobrecarga con Consumer<Builder> (en vez de
        // encadenar .build() nosotros mismos) porque el compilador de
        // Eclipse/ECJ resuelve mal el build() heredado a la vez de
        // S3Request.Builder y CopyableBuilder en esta jerarquia de tipos
        // del SDK de AWS (covariant return ambiguo) y produce una clase
        // rota en tiempo de ejecucion bajo ciertas herramientas basadas en
        // ECJ. javac lo resuelve bien, pero esta forma evita el problema
        // de raiz sin depender de que compilador se use.
        String key = workOrderId + "/" + UUID.randomUUID();
        s3Client.putObject(
                b -> b.bucket(com.floatops.shared.S3BucketInitializer.PHOTOS_BUCKET).key(key),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));

        com.floatops.entity.Photo photo = new com.floatops.entity.Photo();
        photo.workOrder = wo;
        photo.url = key; // guardamos la KEY del objeto, no una URL - se firma al consultar
        photo.timestamp = java.time.LocalDateTime.now();
        wo.photos.add(photo);
        photo.persist();

        return toApiPhoto(photo);
    }

    @Transactional
    public WorkOrder marcarListaParaRevision(UUID id) {
        com.floatops.entity.WorkOrder wo = com.floatops.entity.WorkOrder.findById(id);
        if (wo == null) {
            throw new NotFoundException("Orden no encontrada");
        }
        wo.listaParaRevision = true;
        return toApiWorkOrder(wo);
    }

    // RF-014: agregar nota desde la vista movil.
    @Transactional
    public void agregarNota(UUID workOrderId, String texto) {
        com.floatops.entity.WorkOrder wo = com.floatops.entity.WorkOrder.findById(workOrderId);
        if (wo == null) {
            throw new NotFoundException("Orden no encontrada");
        }

        com.floatops.entity.Note nota = new com.floatops.entity.Note();
        nota.workOrder = wo;
        nota.texto = texto;
        nota.timestamp = java.time.LocalDateTime.now();
        wo.notes.add(nota);
        nota.persist();
    }

    @Transactional
    public WorkOrder aprobarCotizacionCliente(UUID id) {
        com.floatops.entity.WorkOrder wo = com.floatops.entity.WorkOrder.findById(id);
        if (wo == null) {
            throw new NotFoundException("Orden no encontrada");
        }
        boolean anterior = wo.cotizacionAprobadaCliente;
        wo.cotizacionAprobadaCliente = true;
        auditService.registrar("WorkOrder", wo.id, "cotizacionAprobadaCliente", anterior, true);
        return toApiWorkOrder(wo);
    }

    @Transactional
    public WorkOrder aprobarCotizacionSupervisor(UUID id) {
        com.floatops.entity.WorkOrder wo = com.floatops.entity.WorkOrder.findById(id);
        if (wo == null) {
            throw new NotFoundException("Orden no encontrada");
        }
        Boolean anterior = wo.cotizacionAprobadaSupervisor;
        wo.cotizacionAprobadaSupervisor = true;
        auditService.registrar("WorkOrder", wo.id, "cotizacionAprobadaSupervisor", anterior, true);
        return toApiWorkOrder(wo);
    }

    @Transactional
    public com.floatops.dto.generated.ChecklistItem actualizarChecklistItem(UUID workOrderId, UUID itemId, Boolean completado) {
        com.floatops.entity.ChecklistItem item = com.floatops.entity.ChecklistItem.findById(itemId);
        if (item == null || !item.workOrder.id.equals(workOrderId)) {
            throw new NotFoundException("Item de checklist no encontrado en esta orden");
        }
        boolean anterior = item.completado;
        item.completado = Boolean.TRUE.equals(completado);
        auditService.registrar("WorkOrder", workOrderId, "checklist:" + item.descripcion, anterior, item.completado);
        return toApiChecklistItem(item);
    }

    // --- conversiones entidad -> DTO generado ---

    private WorkOrder toApiWorkOrder(com.floatops.entity.WorkOrder wo) {
        WorkOrder dto = new WorkOrder();
        dto.setId(wo.id);
        dto.setVehicleId(wo.vehicle.id);
        dto.setTechnicianId(wo.technician != null ? wo.technician.id : null);
        dto.setStageId(wo.stage.id);
        dto.setPrioridad(com.floatops.dto.generated.Prioridad.valueOf(wo.prioridad.name()));
        dto.setDescripcion(wo.descripcion);
        dto.setFechaProgramada(wo.fechaProgramada);
        dto.setPresupuesto(wo.presupuesto != null ? wo.presupuesto.doubleValue() : null);
        dto.setCotizacionAprobadaCliente(wo.cotizacionAprobadaCliente);
        dto.setCotizacionAprobadaSupervisor(wo.cotizacionAprobadaSupervisor);
        dto.setListaParaRevision(wo.listaParaRevision);

        List<com.floatops.dto.generated.ChecklistItem> checklist = new ArrayList<>();
        for (com.floatops.entity.ChecklistItem item : wo.checklist) {
            checklist.add(toApiChecklistItem(item));
        }
        dto.setChecklist(checklist);

        List<Photo> photos = new ArrayList<>();
        for (com.floatops.entity.Photo photo : wo.photos) {
            photos.add(toApiPhoto(photo));
        }
        dto.setPhotos(photos);

        List<com.floatops.dto.generated.Note> notes = new ArrayList<>();
        for (com.floatops.entity.Note note : wo.notes) {
            com.floatops.dto.generated.Note noteDto = new com.floatops.dto.generated.Note();
            noteDto.setId(note.id);
            noteDto.setTexto(note.texto);
            // ASUMIDO: Note.setTimestamp espera OffsetDateTime, igual que
            // Photo - verificar si no compila.
            noteDto.setTimestamp(note.timestamp.atOffset(ZoneOffset.UTC));
            notes.add(noteDto);
        }
        dto.setNotes(notes);

        return dto;
    }

    private WorkOrderSummary toApiSummary(com.floatops.entity.WorkOrder wo) {
        // ASUMIDO: WorkOrderSummary.set{Id,VehiclePlaca,StageName,
        // TechnicianName,Prioridad,Atrasada} segun el schema original.
        WorkOrderSummary dto = new WorkOrderSummary();
        dto.setId(wo.id);
        dto.setVehiclePlaca(wo.vehicle.placa);
        dto.setStageName(wo.stage.nombre);
        dto.setTechnicianName(wo.technician != null ? wo.technician.nombre : "Sin asignar");
        dto.setPrioridad(com.floatops.dto.generated.Prioridad.valueOf(wo.prioridad.name()));
        dto.setAtrasada(wo.fechaProgramada != null && wo.fechaProgramada.isBefore(LocalDate.now()));
        return dto;
    }

    private com.floatops.dto.generated.ChecklistItem toApiChecklistItem(com.floatops.entity.ChecklistItem item) {
        // ASUMIDO: ChecklistItem.set{Id,Descripcion,Completado,Orden}
        com.floatops.dto.generated.ChecklistItem dto = new com.floatops.dto.generated.ChecklistItem();
        dto.setId(item.id);
        dto.setDescripcion(item.descripcion);
        dto.setCompletado(item.completado);
        dto.setOrden(item.orden);
        return dto;
    }

    private Photo toApiPhoto(com.floatops.entity.Photo photo) {
        // ASUMIDO: Photo.set{Id,Url,Timestamp}. El "timestamp" del schema
        // (format: date-time) probablemente genera OffsetDateTime, no
        // LocalDateTime como en la entidad - convertimos asumiendo UTC.
        // Verificar si no compila.
        Photo dto = new Photo();
        dto.setId(photo.id);
        dto.setUrl(photo.url);
        dto.setTimestamp(photo.timestamp.atOffset(ZoneOffset.UTC));
        return dto;
    }
}