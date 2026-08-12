package com.floatops.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "parts")
public class Part extends BaseEntity {

    @Column(nullable = false, unique = true)
    public String codigo;

    @Column(nullable = false)
    public String nombre;

    @Column(nullable = false)
    public int stock = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal precio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_category_id")
    public PartCategory category;

    public static List<Part> findLowStock(int threshold) {
        return list("stock <= ?1", threshold);
    }
}