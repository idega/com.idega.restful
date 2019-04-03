package com.idega.restful.exception;

import javax.ws.rs.core.Response;

public class NotFound extends RestException{
	private static final long serialVersionUID = 976886295516994145L;

	public NotFound(String message){
		super(message,Response.Status.NOT_FOUND.getStatusCode());
	}
}
