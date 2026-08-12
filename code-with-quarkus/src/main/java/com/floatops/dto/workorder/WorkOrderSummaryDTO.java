package com.floatops.dto.workorder;

// Respuesta de listado (GET /work-orders, RF-004/RF-037) - version
// liviana, sin checklist/fotos/repuestos completos.
public class WorkOrderSummaryDTO {

    public Long id;
    public String vehiclePlaca;
    public String stageName;
    public String technicianName;
    public String prioridad;
    public boolean atrasada;
}
