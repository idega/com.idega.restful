package com.idega.restful.bean;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.idega.util.CoreConstants;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LocalizedText implements JAXBNatural, Cloneable {

	private String key;
	private String defaultValue;

	private Map<String, String> variables;

	public LocalizedText() {
		super();
	}

	public LocalizedText(String key,String defaultValue) {
		this();

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

	public Map<String, String> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, String> variables) {
		this.variables = variables;
	}

	@Override
	public LocalizedText clone() throws CloneNotSupportedException {
		return (LocalizedText) super.clone();
	}

	@Override
	public String toString() {
		return getKey() + CoreConstants.EQ + getDefaultValue();
	}

}