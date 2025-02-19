package com.officemanagement.config;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UnsupportedMediaTypeExceptionMapper implements ExceptionMapper<NotSupportedException> {
    @Override
    public Response toResponse(NotSupportedException exception) {
        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .entity("Unsupported Media Type")
                .build();
    }
}
 