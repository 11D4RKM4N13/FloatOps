package com.floatops.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// RF-025: catalogo base gestionado desde Administracion
@Entity
@Table(name = "part_categories")
public class PartCategory extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String nombre;
}
