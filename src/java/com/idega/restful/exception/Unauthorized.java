package com.idega.restful.exception;

import javax.ws.rs.core.Response;

public class Unauthorized extends RestException{
	private static final long serialVersionUID = -8300132640781603695L;

	public Unauthorized() {
		this(
				"You need to be logged in to use this function."
		);
	}
	public Unauthorized(String message){
		super(message,Response.Status.UNAUTHORIZED.getStatusCode());
	}
}
