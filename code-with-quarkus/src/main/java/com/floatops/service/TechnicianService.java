package com.floatops.service;

import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.floatops.dto.generated.Technician;
import com.floatops.dto.generated.TechnicianCreate;

@ApplicationScoped
public class TechnicianService {

    public List<Technician> listTechnicians() {
        return List.of();
    }

    @Transactional
    public Technician createTechnician(TechnicianCreate dto) {
        com.floatops.entity.Technician entity = new com.floatops.entity.Technician();
        entity.persist();
        return null; 
    }
}