package com.floatops.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

// PanacheEntityBase (no PanacheEntity) porque PanacheEntity fuerza id de
// tipo Long. Los 11 contratos OpenAPI definen los IDs como UUID, asi que
// declaramos nuestro propio id aqui una sola vez y las 10 entidades
// heredan de esta clase en vez de repetir el boilerplate.
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    public UUID id;
}