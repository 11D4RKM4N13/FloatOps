package com.floatops.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;

// RF-035: catalogo configurable, NO un enum fijo en codigo.
// "Requiere atencion", "Programado", "En taller", "Completados" son
// solo la configuracion inicial por defecto (ver V1__init.sql).
@Entity
@Table(name = "stages")
public class Stage extends PanacheEntity {

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
