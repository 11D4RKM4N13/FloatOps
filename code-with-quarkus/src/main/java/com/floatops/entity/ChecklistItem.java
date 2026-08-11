package com.floatops.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem extends PanacheEntity {

    @Column(nullable = false)
    public String descripcion;

    @Column(nullable = false)
    public boolean completado = false;

    @Column(nullable = false)
    public int orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    public WorkOrder workOrder;
}
