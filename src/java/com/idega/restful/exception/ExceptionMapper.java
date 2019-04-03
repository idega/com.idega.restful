package com.idega.restful.exception;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Service;

@Service
@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Throwable> {
    public Response toResponse(Throwable exception) {
    	Logger.getLogger(ExceptionMapper.class.getName()).log(
    			Level.WARNING, 
    			"Exception ", 
    			exception
    	);
    	if(exception instanceof WebApplicationException) {
    		return getExceptionResponse((WebApplicationException) exception);
    	}
    	
    	InternalServerError error = new InternalServerError(
    			"Unexpected error encountered"
    	);
    	return getExceptionResponse(error);
    }
    
    private Response getExceptionResponse(WebApplicationException wex) {
    	return wex.getResponse(); 
    }

}
