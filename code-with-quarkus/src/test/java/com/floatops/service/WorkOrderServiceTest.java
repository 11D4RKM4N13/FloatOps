package com.floatops.service;

import com.floatops.dto.generated.WorkOrder;
import com.floatops.dto.generated.WorkOrderCreate;
import com.floatops.entity.Prioridad;
import com.floatops.entity.ServiceType;
import com.floatops.entity.Stage;
import com.floatops.entity.Vehicle;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NOTA sobre el cambio de estrategia: el intento original con
 * Mockito.mockStatic(WorkOrder.class) fallaba porque findById() esta
 * declarado en PanacheEntityBase (la superclase), no en WorkOrder.class
 * directamente - mockStatic() solo intercepta metodos declarados en la
 * clase exacta que le pasas, asi que nunca capturaba la llamada real.
 * Es un choque conocido entre el mock estatico de Mockito y como
 * Quarkus genera el bytecode de las entidades Panache.
 *
 * Por eso esto es @QuarkusTest con base de datos real (Dev Services,
 * ver application.properties) en vez de mocks. @TestTransaction hace
 * rollback automatico despues de cada test, asi que no hace falta
 * limpieza manual ni los tests se contaminan entre si.
 */
@QuarkusTest
class WorkOrderServiceTest {

    @Inject
    WorkOrderService service;

    private static final BigDecimal UMBRAL_ALTO_VALOR = new BigDecimal("2000000");

    private Vehicle crearVehiculo() {
        Vehicle v = new Vehicle();
        v.placa = "TEST-" + UUID.randomUUID().toString().substring(0, 6);
        v.persist();
        return v;
    }

    private Stage crearEtapa(String nombre, int orden) {
        Stage s = new Stage();
        s.nombre = nombre;
        s.orden = orden;
        s.activo = true;
        s.persist();
        return s;
    }

    private ServiceType crearTipoServicio() {
        ServiceType st = new ServiceType();
        st.nombre = "Tipo de prueba " + UUID.randomUUID();
        st.persist();
        return st;
    }

    private com.floatops.entity.WorkOrder crearOrden(Vehicle vehicle, Stage stage, ServiceType serviceType,
                                                       BigDecimal presupuesto, Boolean aprobadaSupervisor) {
        com.floatops.entity.WorkOrder wo = new com.floatops.entity.WorkOrder();
        wo.vehicle = vehicle;
        wo.stage = stage;
        wo.serviceType = serviceType;
        wo.prioridad = Prioridad.MEDIA;
        wo.descripcion = "Orden de prueba";
        wo.presupuesto = presupuesto;
        wo.cotizacionAprobadaSupervisor = aprobadaSupervisor;
        wo.persist();
        return wo;
    }

    // --- crear() ---

    @Test
    @TestTransaction
    void crear_lanza400_siVehiculoNoExiste() {
        WorkOrderCreate dto = new WorkOrderCreate();
        dto.setVehicleId(UUID.randomUUID()); // no existe
        dto.setTipoServicio("Cualquiera");
        dto.setDescripcion("test");
        dto.setPrioridad(com.floatops.dto.generated.Prioridad.MEDIA);

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> service.crear(dto));

        assertEquals(400, ex.getResponse().getStatus());
    }

    // --- cambiarEtapa(): bloqueo por cotizacion de alto valor (RF-033/RF-039) ---

    @Test
    @TestTransaction
    void cambiarEtapa_bloqueaConCodigo409_siAltoValorYSinAprobarYEntrandoATaller() {
        Vehicle vehicle = crearVehiculo();
        Stage requiereAtencion = crearEtapa("Requiere atencion", 1);
        Stage enTaller = crearEtapa("En taller", 2);
        ServiceType tipo = crearTipoServicio();

        com.floatops.entity.WorkOrder wo = crearOrden(vehicle, requiereAtencion, tipo,
                UMBRAL_ALTO_VALOR.add(BigDecimal.ONE), null);

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> service.cambiarEtapa(wo.id, enTaller.id));

        assertEquals(409, ex.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void cambiarEtapa_permite_siAltoValorPeroYaAprobadaPorSupervisor() {
        Vehicle vehicle = crearVehiculo();
        Stage requiereAtencion = crearEtapa("Requiere atencion", 1);
        Stage enTaller = crearEtapa("En taller", 2);
        ServiceType tipo = crearTipoServicio();

        com.floatops.entity.WorkOrder wo = crearOrden(vehicle, requiereAtencion, tipo,
                UMBRAL_ALTO_VALOR.add(BigDecimal.ONE), true);

        WorkOrder resultado = service.cambiarEtapa(wo.id, enTaller.id);

        assertEquals(enTaller.id, resultado.getStageId());
    }

    @Test
    @TestTransaction
    void cambiarEtapa_permite_siMontoBajoElUmbral_aunqueNoAprobada() {
        Vehicle vehicle = crearVehiculo();
        Stage requiereAtencion = crearEtapa("Requiere atencion", 1);
        Stage enTaller = crearEtapa("En taller", 2);
        ServiceType tipo = crearTipoServicio();

        com.floatops.entity.WorkOrder wo = crearOrden(vehicle, requiereAtencion, tipo,
                new BigDecimal("500000"), null);

        WorkOrder resultado = service.cambiarEtapa(wo.id, enTaller.id);

        assertEquals(enTaller.id, resultado.getStageId());
    }

    @Test
    @TestTransaction
    void cambiarEtapa_permite_siAltoValorPeroEtapaDistintaDeEnTaller() {
        Vehicle vehicle = crearVehiculo();
        Stage requiereAtencion = crearEtapa("Requiere atencion", 1);
        Stage programado = crearEtapa("Programado", 2);
        ServiceType tipo = crearTipoServicio();

        com.floatops.entity.WorkOrder wo = crearOrden(vehicle, requiereAtencion, tipo,
                UMBRAL_ALTO_VALOR.add(BigDecimal.ONE), null);

        WorkOrder resultado = service.cambiarEtapa(wo.id, programado.id);

        assertEquals(programado.id, resultado.getStageId());
    }

    @Test
    @TestTransaction
    void cambiarEtapa_lanza404_siOrdenNoExiste() {
        Stage stage = crearEtapa("Programado", 1);

        assertThrows(NotFoundException.class,
                () -> service.cambiarEtapa(UUID.randomUUID(), stage.id));
    }

    // --- obtener() ---

    @Test
    @TestTransaction
    void obtener_lanza404_siOrdenNoExiste() {
        assertThrows(NotFoundException.class, () -> service.obtener(UUID.randomUUID()));
    }

    @Test
    @TestTransaction
    void obtener_devuelveDTO_siOrdenExiste() {
        Vehicle vehicle = crearVehiculo();
        Stage stage = crearEtapa("Requiere atencion", 1);
        ServiceType tipo = crearTipoServicio();
        com.floatops.entity.WorkOrder wo = crearOrden(vehicle, stage, tipo, null, null);

        WorkOrder resultado = service.obtener(wo.id);

        assertEquals(wo.id, resultado.getId());
        assertEquals(wo.descripcion, resultado.getDescripcion());
    }

    // --- marcarListaParaRevision() (RF-015) ---

    @Test
    @TestTransaction
    void marcarListaParaRevision_actualizaElCampo() {
        Vehicle vehicle = crearVehiculo();
        Stage stage = crearEtapa("Requiere atencion", 1);
        ServiceType tipo = crearTipoServicio();
        com.floatops.entity.WorkOrder wo = crearOrden(vehicle, stage, tipo, null, null);

        WorkOrder resultado = service.marcarListaParaRevision(wo.id);

        assertTrue(resultado.getListaParaRevision());
    }
}