package com.floatops.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "work_order_parts")
public class WorkOrderPart extends BaseEntity {

    @Column(nullable = false)
    public int cantidad = 1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    public WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    public Part part;
}