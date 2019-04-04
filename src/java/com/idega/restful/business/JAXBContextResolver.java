package com.idega.restful.business;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.springframework.stereotype.Service;

import com.idega.restful.bean.JAXBNatural;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

@Service
@Provider
public class JAXBContextResolver implements ContextResolver<JAXBContext> {
    @Override
    public JAXBContext getContext(Class<?> objectType) {
    	if (!JAXBNatural.class.isAssignableFrom(objectType)) {
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
}
