package com.floatops.resource;

import java.util.List;
import java.util.UUID;

import com.floatops.dto.generated.EstadoVehiculo;
import com.floatops.dto.generated.ListVehicleWorkOrders200ResponseInner;
import com.floatops.dto.generated.ListVehicles200Response;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

// Importas la interfaz y los DTOs generados

import com.floatops.resource.generated.VehiclesApi;
import com.floatops.dto.generated.VehicleCreate;
import com.floatops.dto.generated.Vehicle;
import com.floatops.service.VehicleService;

public class VehicleResource implements VehiclesApi {

    @Override
    public Vehicle createVehicle(VehicleCreate vehicleCreate) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vehicle deactivateVehicle(UUID vehicleId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vehicle getVehicle(UUID vehicleId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<ListVehicleWorkOrders200ResponseInner> listVehicleWorkOrders(UUID vehicleId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListVehicles200Response listVehicles(UUID clientId, String tipo, EstadoVehiculo estado, String estadoMantenimiento, Integer page, Integer size) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vehicle updateVehicle(UUID vehicleId, VehicleCreate vehicleCreate) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

   
}