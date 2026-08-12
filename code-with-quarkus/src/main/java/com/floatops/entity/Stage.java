package com.floatops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;

@Entity
@Table(name = "stages")
public class Stage extends BaseEntity {

    @Column(nullable = false)
    public String nombre;

    @Column(nullable = false)
    public int orden;

    @Column(nullable = false)
    public boolean activo = true;

    public static List<Stage> findAllActiveOrdered() {
        return list("activo = true order by orden");
    }
}