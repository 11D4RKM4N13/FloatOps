package com.floatops.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

// STUB: solo lo minimo para que WorkOrder compile y tenga su FK.
// El resto de campos (placa ya esta, vin, marca, modelo, kilometraje, Client)
// se completan en el modulo Vehiculos (RF-016 a RF-019, RF-034).
@Entity
@Table(name = "vehicles")
public class Vehicle extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String placa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EstadoVehiculo estado = EstadoVehiculo.ACTIVO;
}
