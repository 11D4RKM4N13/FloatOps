package com.floatops.resource;

import java.util.List;
import java.util.UUID;

import com.floatops.dto.generated.Technician;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.floatops.resource.generated.TechniciansApi;
import com.floatops.dto.generated.TechnicianCreate;
import com.floatops.service.TechnicianService;

public class TechnicianResource implements TechniciansApi {

    @Override
    public Technician createTechnician(TechnicianCreate technicianCreate) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Technician getTechnician(UUID technicianId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Technician> listTechnicians(Boolean disponible) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Technician updateTechnician(UUID technicianId, TechnicianCreate technicianCreate) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}