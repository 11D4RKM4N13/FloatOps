package com.floatops.dto.workorder;

// Body de PATCH /work-orders/{id}/stage - RF-005 (drag&drop) y RF-031
// (boton en el detalle) usan el mismo endpoint.
public class ChangeStageDTO {
    public Long stageId;
}
