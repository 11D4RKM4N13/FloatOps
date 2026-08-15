package com.floatops.shared.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

// Sin esto, "throw new WebApplicationException("mensaje", status)" en
// cualquier Service pone el mensaje solo en logs internos del servidor
// - el cliente recibe el status code correcto pero el body vacio.
// NotFoundException extiende WebApplicationException, asi que este
// mismo mapper tambien cubre los 404 (ver JAX-RS resolucion por tipo).
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse().getStatus();
        return Response.status(status)
                .entity(new ErrorResponse(exception.getMessage(), status))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}