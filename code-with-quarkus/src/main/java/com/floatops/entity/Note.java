package com.floatops.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// RF-014: notas de texto libre agregadas desde la vista movil del tecnico.
@Entity
@Table(name = "notes")
public class Note extends BaseEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    public String texto;

    @Column(nullable = false)
    public LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    public WorkOrder workOrder;
}