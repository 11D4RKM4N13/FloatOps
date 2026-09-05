package com.floatops.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.floatops.dto.part.PartCreateDTO;
import com.floatops.entity.Part;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class PartService {

    // RF-022: Consultar catalogo de repuestos
    public List<Part> listParts(UUID categoryId, String search) {

        if (categoryId != null) {
            return Part.list("category.id", categoryId);
        }

        if (search != null && !search.isBlank()) {
            String searchValue = "%" + search.toLowerCase() + "%";

            return Part.list(
                    "lower(nombre) like ?1 or lower(codigo) like ?1",
                    searchValue
            );
        }

        return Part.listAll();
    }

    // RF-022: Registrar repuesto
    @Transactional
    public Part createPart(PartCreateDTO dto) {

        // Validar codigo duplicado
        Part existingPart = Part.find("codigo", dto.code).firstResult();

        if (existingPart != null) {
            throw new WebApplicationException(
                    "Part code already exists",
                    Response.Status.CONFLICT
            );
        }

        // Validar precio
        if (dto.price == null || dto.price.compareTo(BigDecimal.ZERO) < 0) {
            throw new WebApplicationException(
                    "Price must be greater than or equal to zero",
                    Response.Status.BAD_REQUEST
            );
        }

        // Validar stock
        if (dto.stock < 0) {
            throw new WebApplicationException(
                    "Stock cannot be negative",
                    Response.Status.BAD_REQUEST
            );
        }

        Part part = new Part();

        part.codigo = dto.code;
        part.nombre = dto.name;
        part.precio = dto.price;
        part.stock = dto.stock;

        // Category is optional for now
        if (dto.categoryId != null) {
            part.category = com.floatops.entity.PartCategory.findById(dto.categoryId);

            if (part.category == null) {
                throw new WebApplicationException(
                        "Part category not found",
                        Response.Status.BAD_REQUEST
                );
            }
        }

        part.persist();

        return part;
    }

    // RF-022: Actualizar stock
    @Transactional
    public Part updateStock(UUID partId, int stock) {

        Part part = Part.findById(partId);

        if (part == null) {
            throw new NotFoundException("Part not found");
        }

        if (stock < 0) {
            throw new WebApplicationException(
                    "Stock cannot be negative",
                    Response.Status.BAD_REQUEST
            );
        }

        part.stock = stock;

        return part;
    }
}