package com.floatops.dto.workorder;

import java.math.BigDecimal;
import java.time.LocalDate;

// Respuesta de detalle (GET /work-orders/{id}, RF-011).
// Nunca devolvemos la entidad JPA directo - evita problemas de
// lazy-loading al serializar y desacopla el contrato publico del
// modelo interno.
public class WorkOrderDTO {

    public Long id;
    public Long vehicleId;
    public Long technicianId;
    public Long stageId;
    public String stageName;
    public String prioridad;
    public String descripcion;
    public LocalDate fechaProgramada;
    public BigDecimal presupuesto;
    public boolean cotizacionAprobadaCliente;
    public Boolean cotizacionAprobadaSupervisor;
    public boolean listaParaRevision;
}
