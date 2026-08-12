package com.floatops.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    @Column(nullable = false, unique = true)
    public String placa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EstadoVehiculo estado = EstadoVehiculo.ACTIVO;
}