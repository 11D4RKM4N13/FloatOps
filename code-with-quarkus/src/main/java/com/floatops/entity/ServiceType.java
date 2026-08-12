package com.floatops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_types")
public class ServiceType extends BaseEntity {

    @Column(nullable = false, unique = true)
    public String nombre;
}