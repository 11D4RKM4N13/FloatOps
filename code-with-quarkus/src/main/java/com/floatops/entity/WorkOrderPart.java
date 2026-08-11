package com.floatops.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

// Tabla intermedia explicita (no una simple N-N) para poder llevar
// atributos propios como cantidad, y precio_al_momento en el futuro.
@Entity
@Table(name = "work_order_parts")
public class WorkOrderPart extends PanacheEntity {

    @Column(nullable = false)
    public int cantidad = 1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    public WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    public Part part;
}
