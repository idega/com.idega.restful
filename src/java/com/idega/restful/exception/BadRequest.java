package com.idega.restful.exception;

import javax.ws.rs.core.Response;

public class BadRequest extends RestException{
	private static final long serialVersionUID = -8300132640781603695L;

	public BadRequest(String message){
		super(message,Response.Status.BAD_REQUEST.getStatusCode());
	}
}
