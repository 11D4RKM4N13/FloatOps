package com.floatops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "part_categories")
public class PartCategory extends BaseEntity {

    @Column(nullable = false, unique = true)
    public String nombre;
}