package com.idega.restful.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.google.gson.Gson;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.restful.RestfulConstants;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

public abstract class DefaultRestfulService extends DefaultSpringBean {

	@GET
	@Path(RestfulConstants.URI_LOCALIZED)
	public Response getLozalizedString(
			@QueryParam("key") String key,
			@QueryParam("default_text") String defaultText,
			@QueryParam("bundle") String bundleIdentifier,
			@QueryParam("locale") String localeId
	) {
		String message = null;
		if (StringUtil.isEmpty(key) || StringUtil.isEmpty(bundleIdentifier)) {
			message = "Provide valid arguments: key " + key + ", bundle: " + bundleIdentifier;
			return getResponse(Response.Status.BAD_REQUEST, message);
		}

		Locale locale = null;
		try {
			locale = StringUtil.isEmpty(localeId) ? getCurrentLocale() : ICLocaleBusiness.getLocaleFromLocaleString(localeId);
			IWBundle bundle = getBundle(bundleIdentifier);
			IWResourceBundle iwrb = bundle.getResourceBundle(locale);
			message = iwrb.getLocalizedString(key, StringUtil.isEmpty(defaultText) ? key : defaultText);
			return getOKResponse(message);
		} catch (Exception e) {
			message = "Error getting localized string for keyword " + key + ", default text: " + defaultText + ", bundle: " +
					bundleIdentifier + " and locale: " + locale;
			getLogger().log(Level.WARNING, message, e);
			return getResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
		}
	}

	protected boolean isOKResponse(Response response) {
		return response != null && Response.Status.OK.getStatusCode() == response.getStatus();
	}

	protected Response getResponse(Response.Status status, Serializable message, AdvancedProperty... headers) {
		ResponseBuilder responseBuilder = Response.status(status.getStatusCode());
		responseBuilder = responseBuilder.entity(getJSON(message));
		if (!ArrayUtil.isEmpty(headers)) {
			for (AdvancedProperty header: headers) {
				responseBuilder.header(header.getName(), header.getValue());
			}
		}
		Response response = responseBuilder.build();
		return response;
	}

	protected <T extends Serializable> Response getResponse(Response.Status status, List<T> entities, AdvancedProperty... headers) {
		Serializable message = ListUtil.isEmpty(entities) ? new ArrayList<T>(0) : new ArrayList<T>(entities);
		return getResponse(status, message, headers);
	}

	protected Response getOKResponse(Serializable message, AdvancedProperty... headers) {
		return getResponse(Response.Status.OK, message);
	}
	protected <E extends Serializable> Response getOKResponse(List<E> entities) {
		Serializable message = ListUtil.isEmpty(entities) ? new ArrayList<E>(0) : new ArrayList<E>(entities);
		return getResponse(Response.Status.OK, message);
	}

	protected Response getBadRequestResponse(Serializable message, AdvancedProperty... headers) {
		return getResponse(Response.Status.BAD_REQUEST, message);
	}

	protected Response getInternalServerErrorResponse(Serializable message, AdvancedProperty... headers) {
		return getResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
	}

    protected String getJSON(Serializable object) {
		Gson gson = new Gson();
		return gson.toJson(object);
	}

    protected User getUser(String userId) {
    	if (StringUtil.isEmpty(userId))
    		return null;

    	User user = null;
    	UserBusiness userBusiness = getServiceInstance(UserBusiness.class);
    	try {
			user = userBusiness.getUser(userId);
		} catch (Exception e) {}

    	if (user == null) {
    		try {
    			user = userBusiness.getUser(Integer.valueOf(userId));
    		} catch (Exception e) {}
    	}

    	if (user == null)
    		getLogger().warning("Error getting user by ID: " + userId);

    	return user;
	}

    protected boolean logInUser(User user) {
    	if (user == null) {
    		return Boolean.FALSE;
    	}

    	IWContext iwc = CoreUtil.getIWContext();
    	if (iwc == null) {
    		return Boolean.FALSE;
    	}

    	if (iwc.isLoggedOn()) {
    		return Boolean.TRUE;
    	}

    	try {
			return LoginBusinessBean.getDefaultLoginBusinessBean()
					.logInByPersonalID(CoreUtil.getIWContext(), user.getPersonalID());
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Unable to login: ", e);
		}

    	return Boolean.FALSE;
    }
}