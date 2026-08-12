package com.floatops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "technicians")
public class Technician extends BaseEntity {

    @Column(nullable = false)
    public String nombre;
}