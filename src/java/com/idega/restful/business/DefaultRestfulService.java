package com.idega.restful.business;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.ReflectionUtils;

import com.google.gson.Gson;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.restful.RestfulConstants;
import com.idega.servlet.filter.RequestResponseProvider;
import com.idega.user.business.StandardGroup;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
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

	protected Response getResponse(int statusCode, Serializable message, AdvancedProperty... headers) {
		return getResponse(Response.Status.fromStatusCode(statusCode), message, headers);
	}
	protected Response getResponse(Response.Status status, Object data, AdvancedProperty... headers) {
		ResponseBuilder responseBuilder = Response.status(status.getStatusCode());

		if (data instanceof byte[]) {

			StreamingOutput stream = new StreamingOutput() {

				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						output.write((byte[]) data);
					} catch (Exception e) {
						throw new WebApplicationException(e);
					}
				}

		    };

		    responseBuilder = Response.ok(stream);
		} else if (data instanceof Serializable) {
			responseBuilder = responseBuilder.entity(getJSON((Serializable) data));
		}

		if (!ArrayUtil.isEmpty(headers)) {
			for (AdvancedProperty header: headers) {
				responseBuilder.header(header.getName(), header.getValue());
			}
		}

		boolean jSessionCookieSet = false;
		String jSessionIDName = "JSESSIONID";
		IWContext iwc = CoreUtil.getIWContext();
		List<NewCookie> newCookies = new ArrayList<>();
		if (iwc != null) {
			Cookie[] cookies = iwc.getCookies();
			if (!ArrayUtil.isEmpty(cookies)) {
				for (Cookie cookie: cookies) {
					String name = cookie.getName();
					if (!jSessionCookieSet && name != null) {
						jSessionCookieSet = name.equals(jSessionIDName);
					}
					newCookies.add(new NewCookie(name, cookie.getValue()));
				}
			}
		}
		if (!jSessionCookieSet) {
			try {
				RequestResponseProvider rrProvider = ELUtil.getInstance().getBean(RequestResponseProvider.class);
				HttpServletRequest request = rrProvider.getRequest();
				if (request != null) {
					HttpSession session = request.getSession();
					if (session != null) {
						String sessionId = session.getId();
						if (StringUtil.isEmpty(sessionId)) {

						} else {
							newCookies.add(new NewCookie(jSessionIDName, sessionId));
						}
					}
				}
			} catch (Exception e) {}
		}
		if (!ListUtil.isEmpty(newCookies)) {
			responseBuilder.cookie(ArrayUtil.convertListToArray(newCookies));
		}

		Response response = responseBuilder.build();
		return response;
	}

	protected <T extends Serializable> Response getResponse(Response.Status status, List<T> entities, AdvancedProperty... headers) {
		Serializable message = ListUtil.isEmpty(entities) ? new ArrayList<T>(0) : new ArrayList<T>(entities);
		return getResponse(status, message, headers);
	}

	protected Response getOKResponse(byte[] bytes, AdvancedProperty... headers) {
		return getResponse(Response.Status.OK, bytes, headers);
	}
	protected Response getOKResponse(Serializable message, AdvancedProperty... headers) {
		return getResponse(Response.Status.OK, message, headers);
	}
	protected <E extends Serializable> Response getOKResponse(List<E> entities) {
		Serializable message = ListUtil.isEmpty(entities) ? new ArrayList<E>(0) : new ArrayList<E>(entities);
		return getResponse(Response.Status.OK, message);
	}

	protected Response getBadRequestResponse(Serializable message, AdvancedProperty... headers) {
		return getResponse(Response.Status.BAD_REQUEST, message, headers);
	}

	protected Response getInternalServerErrorResponse(Serializable message, AdvancedProperty... headers) {
		return getResponse(Response.Status.INTERNAL_SERVER_ERROR, message, headers);
	}

	protected Response getServiceUnavailableResponse(Serializable message, AdvancedProperty... headers) {
		return getResponse(Response.Status.SERVICE_UNAVAILABLE, message, headers);
	}

	protected Response getUnauthorizedResponse(Serializable message, AdvancedProperty... headers) {
		return getResponse(Response.Status.UNAUTHORIZED, message, headers);
	}

	protected Response getUnauthorizedResponse(Serializable message, String path, String method, AdvancedProperty... headers) {
		message = message.toString().concat(". Path: ").concat(path).concat(method.startsWith(CoreConstants.SLASH) ? method : CoreConstants.SLASH.concat(method));
		return getResponse(Response.Status.UNAUTHORIZED, message, headers);
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
		if (standardGroup == null) {
			ELUtil.getInstance().autowire(this);
		}
		return standardGroup;
	}

    protected User getUser(String userId, boolean createIfDoesNotExist) {
    	if (StringUtil.isEmpty(userId)) {
			return null;
		}

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

    protected boolean logInUser(IWContext iwc, User user) {
    	if (user == null) {
    		return Boolean.FALSE;
    	}

    	if (iwc == null) {
    		return Boolean.FALSE;
    	}

    	if (iwc.isLoggedOn()) {
    		return Boolean.TRUE;
    	}

    	try {
			return LoginBusinessBean.getDefaultLoginBusinessBean().logInByPersonalID(iwc, user.getPersonalID());
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Unable to login: ", e);
		}

    	return Boolean.FALSE;
    }

    private String getCurrentMethodName() {
		try {
			final StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
			if (ArrayUtil.isEmpty(stElements) || stElements.length < 4) {
				getLogger().warning("Stack trace is not available");
				return null;
			}

			StackTraceElement ste = stElements[3];
			return ste.getMethodName();
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting current method's name", e);
		}
		return null;
	}

	protected <T> T execute(Object target, Object... args) {
		if (target == null) {
			return null;
		}

		String methodName = null;
		try {
			methodName = getCurrentMethodName();

			Method[] methods = target.getClass().getMethods();
			if (ArrayUtil.isEmpty(methods)) {
				return null;
			}

			for (Method m: methods) {
				if (methodName.equals(m.getName())) {
					@SuppressWarnings("unchecked")
					T result = (T) ReflectionUtils.invokeMethod(m, target, args);
					return result;
				}
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing method '" + methodName + "' at " + target +
					(ArrayUtil.isEmpty(args) ? CoreConstants.EMPTY : " with arguments: " + Arrays.asList(args)), e);
		}

		return null;
	}

	protected boolean parseFlag(String flag) {
		return (
				(flag == null)
				|| ("false".equalsIgnoreCase(flag))
		) ? false : true;
	}

}