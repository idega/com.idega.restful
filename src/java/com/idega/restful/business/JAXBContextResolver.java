package com.idega.restful.business;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.springframework.stereotype.Service;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

@Service
@Provider
public class JAXBContextResolver implements ContextResolver<JAXBContext> {
	private Set<Class<?>> types = new HashSet<Class<?>>();
	
    @Override
    public JAXBContext getContext(Class<?> objectType) {
    	if(!types.contains(objectType)) {
    		return null;
    	}
    	try {
			return new JSONJAXBContext(
					JSONConfiguration.natural().build(),
					objectType
			);
		} catch (JAXBException e) {
			Logger.getLogger(JAXBContextResolver.class.getName()).log(
					Level.WARNING,
					"failed creating context", 
					e
			);
		}
    	return null;
    }
    
    public void addContext(Class<?> type) throws JAXBException{
    	types.add(type);
    }
}
