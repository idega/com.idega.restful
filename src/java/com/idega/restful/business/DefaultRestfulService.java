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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.gson.Gson;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.restful.RestfulConstants;
import com.idega.user.business.StandardGroup;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

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

	protected Response getResponse(Response.Status status, Serializable message) {
		ResponseBuilder responseBuilder = Response.status(status.getStatusCode());
		Response response = responseBuilder.entity(getJSON(message)).build();
		return response;
	}

	protected Response getOKResponse(Serializable message) {
		return getResponse(Response.Status.OK, message);
	}
	protected <E extends Serializable> Response getOKResponse(List<E> entities) {
		return getResponse(Response.Status.OK, ListUtil.isEmpty(entities) ? new ArrayList<E>(0) : new ArrayList<E>(entities));
	}

	protected Response getBadRequestResponse(Serializable message) {
		return getResponse(Response.Status.BAD_REQUEST, message);
	}

	protected Response getInternalServerErrorResponse(Serializable message) {
		return getResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
	}

    protected String getJSON(Serializable object) {
		Gson gson = new Gson();
		return gson.toJson(object);
	}

    protected User getUser(String userId) {
    	return getUser(userId, false);
    }

    @Autowired(required=false)
    @Qualifier("citizenStandardGroup")
	private StandardGroup standardGroup;

	private StandardGroup getStandardGroup() {
		if (standardGroup == null)
			ELUtil.getInstance().autowire(this);
		return standardGroup;
	}

    protected User getUser(String userId, boolean createIfDoesNotExist) {
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

    	if (user == null) {
    		if (createIfDoesNotExist) {
    			try {
	    			StandardGroup standardGroup = getStandardGroup();
	    			String login = userId;
	    			if (login.length() > 32) {
	    				login = login.substring(0, 31);
	    			}
	    			user = userBusiness.createUserWithLogin(
	    					"User",
	    					null,
	    					userId,
	    					null,
	    					null,
	    					null,
	    					null,
	    					standardGroup == null ? null : Integer.valueOf(standardGroup.getGroup().getId()),
	    					login,
	    					userId,
	    					true,
	    					null,
	    					-1,
	    					Boolean.FALSE,
	    					Boolean.TRUE,
	    					Boolean.TRUE,
	    					null
	    			);
	    			if (userId.length() >= 10) {
	    				user.setPersonalID(userId);
	    			}
	    			user.store();
	    			return user;
	    		} catch (Exception e) {
	    			getLogger().log(Level.WARNING, "Error creating user by ID: " + userId, e);
	    		}
    		} else {
    			getLogger().warning("Error getting user by ID: " + userId);
    		}
    	}

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