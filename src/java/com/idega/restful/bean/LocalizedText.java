package com.idega.restful.bean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LocalizedText implements JAXBNatural {
	
	private String key;
	private String defaultValue;
	
	public LocalizedText() {
		
	}
	
	public LocalizedText(String key,String defaultValue) {
		this.key = key;
		this.defaultValue = defaultValue;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

}
