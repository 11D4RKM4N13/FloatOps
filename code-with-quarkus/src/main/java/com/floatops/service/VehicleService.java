package com.floatops.service;

import java.util.List;

import com.floatops.dto.generated.Vehicle;
import com.floatops.dto.generated.VehicleCreate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class VehicleService {

    public List<Vehicle> listVehicles(String search) {
        return List.of(); 
    }

    @Transactional
    public Vehicle createVehicle(VehicleCreate dto) {
        com.floatops.entity.Vehicle vehicleEntity = new com.floatops.entity.Vehicle();
        vehicleEntity.persist();
        return null;
    }
}