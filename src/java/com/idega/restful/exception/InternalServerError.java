package com.idega.restful.exception;

import javax.ws.rs.core.Response;

public class InternalServerError extends RestException{
	private static final long serialVersionUID = 5272787419215106371L;

	public InternalServerError(String message){
		super(message,Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
	}
}
