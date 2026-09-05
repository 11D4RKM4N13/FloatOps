package com.floatops.resource;

import java.util.List;
import java.util.UUID;

import com.floatops.dto.part.PartCreateDTO;
import com.floatops.dto.part.UpdatePartStockDTO;
import com.floatops.entity.Part;
import com.floatops.service.PartService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/parts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartResource {

    @Inject
    PartService partService;

    // RF-022: Consultar catalogo de repuestos
    @GET
    public List<Part> listParts(
            @QueryParam("categoryId") UUID categoryId,
            @QueryParam("search") String search
    ) {
        return partService.listParts(categoryId, search);
    }

    // RF-022: Registrar repuesto
    @POST
    public Response createPart(PartCreateDTO dto) {

        Part part = partService.createPart(dto);

        return Response
                .status(Response.Status.CREATED)
                .entity(part)
                .build();
    }

    // RF-022: Actualizar stock
    @PATCH
    @Path("/{partId}/stock")
    public Part updateStock(
            @PathParam("partId") UUID partId,
            UpdatePartStockDTO dto
    ) {
        return partService.updateStock(partId, dto.stock);
    }
}