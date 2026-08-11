package com.floatops.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// STUB: solo lo minimo para que WorkOrder compile y tenga su FK.
// especialidad, disponibilidad se completan en el modulo Tecnicos
// (RF-020, RF-021).
@Entity
@Table(name = "technicians")
public class Technician extends PanacheEntity {

    @Column(nullable = false)
    public String nombre;
}
