package com.floatops.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "work_orders")
public class WorkOrder extends PanacheEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Prioridad prioridad;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String descripcion;

    @Column(name = "fecha_programada")
    public LocalDate fechaProgramada;

    @Column(precision = 12, scale = 2)
    public BigDecimal presupuesto;

    // RF-033: aprobacion del cliente, registrada manualmente por el despachador
    @Column(name = "cotizacion_aprobada_cliente", nullable = false)
    public boolean cotizacionAprobadaCliente = false;

    // RF-039: null = la cotizacion nunca supero el umbral de alto valor,
    // no confundir con false (supero el umbral pero aun no se aprueba)
    @Column(name = "cotizacion_aprobada_supervisor")
    public Boolean cotizacionAprobadaSupervisor;

    // RF-015: el tecnico marca esto desde movil; NO cambia la etapa.
    // Completar la orden es una accion separada del despachador (RF-031).
    @Column(name = "lista_para_revision", nullable = false)
    public boolean listaParaRevision = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    public Vehicle vehicle;

    // Nullable: el modal rapido (RF-006) puede crear la orden sin tecnico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    public Technician technician;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    public Stage stage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_type_id", nullable = false)
    public ServiceType serviceType;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ChecklistItem> checklist = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Photo> photos = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<WorkOrderPart> parts = new ArrayList<>();

    // RF-004 / kanban agrupado por etapa
    public static List<WorkOrder> findByStage(Long stageId) {
        return list("stage.id", stageId);
    }

    // RF-032 reasignacion / ordenes de un tecnico
    public static List<WorkOrder> findByTechnician(Long technicianId) {
        return list("technician.id", technicianId);
    }

    // RF-037 cola de ordenes abiertas: todo lo que no esta en la etapa "Completados"
    public static List<WorkOrder> findOpen() {
        return list("stage.nombre != ?1", "Completados");
    }
}