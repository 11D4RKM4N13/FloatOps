package com.floatops.dto.workorder;

import java.time.LocalDate;
import java.util.List;

// Cubre RF-006 (modal rapido) y RF-007 (formulario completo).
// technicianId y partIds son opcionales: el modal rapido no los pide,
// el formulario completo si (ver openapi-ordenes-trabajo.yaml).
public class WorkOrderCreateDTO {

    public Long vehicleId;
    public Long serviceTypeId;
    public String descripcion;
    public String prioridad; // ALTA | MEDIA | BAJA
    public LocalDate fechaProgramada;
    public Long technicianId;
    public List<Long> partIds;
}
