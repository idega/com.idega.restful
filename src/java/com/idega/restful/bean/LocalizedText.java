package com.idega.restful.bean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.idega.util.CoreConstants;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LocalizedText implements JAXBNatural, Cloneable {

	private String key;
	private String defaultValue;

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

	@Override
	public LocalizedText clone() throws CloneNotSupportedException {
		return (LocalizedText)super.clone();
	}

	@Override
	public String toString() {
		return getKey() + CoreConstants.EQ + getDefaultValue();
	}

}