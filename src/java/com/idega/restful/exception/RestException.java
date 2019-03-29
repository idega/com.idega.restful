package com.idega.restful.exception;

import java.sql.Timestamp;
import java.util.HashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

public class RestException extends WebApplicationException{
	private static final long serialVersionUID = -2972802302728520710L;
	public RestException(String message, int status){
		super(
				Response.status(status)
				.type(MediaType.APPLICATION_JSON)
				.entity(getInfo(message))
				.build()
		);
	}
	
	private static Object getInfo(String message) {
		HashMap<String, String> info = new HashMap<>();
		info.put("error", message);
		info.put("time", new Timestamp(System.currentTimeMillis()).toString());
		
		return new Gson().toJson(info);
	}
}
