package com.idega.restful.exception;

import java.sql.Timestamp;
import java.util.HashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

public class RestException extends WebApplicationException{
	private static final long serialVersionUID = -2972802302728520710L;
	
	private boolean logged = false;
	
	private String message = null;
	
	public RestException(String message, int status){
		super(
				Response.status(status)
				.type(MediaType.APPLICATION_JSON)
				.entity(getInfo(message))
				.build()
		);
		this.message = message;
	}
	
	private static Object getInfo(String message) {
		HashMap<String, String> info = new HashMap<>();
		info.put("error", message);
		info.put("time", new Timestamp(System.currentTimeMillis()).toString());
		
		return new Gson().toJson(info);
	}

	public boolean isLogged() {
		return logged;
	}

	public void setLogged(boolean logged) {
		this.logged = logged;
	}

	@Override
	public Response getResponse() {
		if(!isLogged()) {
			Response response = new Response() {
				
				@Override
				public int getStatus() {
					// TODO Auto-generated method stub
					return 0;
				}
				
				@Override
				public MultivaluedMap<String, Object> getMetadata() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public Object getEntity() {
					// TODO Auto-generated method stub
					return null;
				}
			};
			return response;
		}
		return super.getResponse();
	}

	@Override
	public String getMessage() {
		return this.message;
	}
}
