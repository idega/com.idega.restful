package com.idega.restful.exception;

import javax.ws.rs.core.Response;

public class Forbidden extends RestException{
	private static final long serialVersionUID = -8300132640781603695L;

	public Forbidden() {
		this(
				"You are not allowed to use this function"
		);
	}
	public Forbidden(String message){
		super(message,Response.Status.FORBIDDEN.getStatusCode());
	}
}
