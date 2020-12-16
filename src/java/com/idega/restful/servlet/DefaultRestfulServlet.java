package com.idega.restful.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.core.localisation.business.LocaleSwitcher;
import com.idega.idegaweb.IWMainApplication;
import com.idega.presentation.IWContext;
import com.idega.restful.spring.container.IWSpringComponentProviderFactory;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.RequestUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

public class DefaultRestfulServlet extends SpringServlet {

	private static final long serialVersionUID = 8737746855252117898L;

	private static final Logger LOGGER = Logger.getLogger(DefaultRestfulServlet.class.getName());

	@Override
	protected void initiate(ResourceConfig rc, WebApplication wa) {
		try {
            wa.initiate(rc, new IWSpringComponentProviderFactory(rc, getContext()));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred when intialization", e);
            throw e;
        }
	}

	@Override
	public int service(URI baseUri, URI requestUri, HttpServletRequest request,	HttpServletResponse response) throws ServletException, IOException {
		initializeContext(request, response);
		try {
			return super.service(baseUri, requestUri, request, response);
		} catch (Exception e) {}
		return Status.INTERNAL_SERVER_ERROR.getStatusCode();
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		initializeContext(request, response);
		super.service(request, response);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		initializeContext(request, response);
		super.doFilter(request, response, chain);
	}

	protected void initializeContext(ServletRequest request, ServletResponse response) {
		IWContext iwc = new IWContext((HttpServletRequest) request, (HttpServletResponse) response, getServletContext());

		//	Checking if locale's parameter is provided
		String localeString = request.getParameter(LocaleSwitcher.languageParameterString);

		//	Checking if locale's parameter is provided in header
		if (StringUtil.isEmpty(localeString) && request instanceof HttpServletRequest) {
			localeString = ((HttpServletRequest) request).getHeader(RequestUtil.HEADER_ACCEPT_LANGUAGE);
		}

		if (StringUtil.isEmpty(localeString) && request instanceof HttpServletRequest) {
			Cookie[] cookies = ((HttpServletRequest) request).getCookies();
			if (!ArrayUtil.isEmpty(cookies)) {
				String localeCookie = "currentLocale";
				for (Cookie cookie: cookies) {
					String name = cookie == null ? null : cookie.getName();
					if (StringUtil.isEmpty(name)) {
						continue;
					}

					if (localeCookie.equals(name)) {
						localeString = cookie.getValue();
					}
				}
			}
		}

		Locale locale = null;
		//	Making sure locale with language and country will be used
		if (!StringUtil.isEmpty(localeString)) {
			localeString = StringHandler.replace(localeString, CoreConstants.MINUS, CoreConstants.UNDER);

			locale = ICLocaleBusiness.getLocaleFromLocaleString(localeString);
			if (locale != null) {
				String language = locale.getLanguage();
				if (!Locale.ENGLISH.getLanguage().equals(language) && StringUtil.isEmpty(locale.getCountry())) {
					//	Locale is not English and without country, trying to find country for given language
					List<Locale> localesWithCountry = ICLocaleBusiness.getLocalesForLanguage(language);
					if (!ListUtil.isEmpty(localesWithCountry)) {
						locale = localesWithCountry.get(0);
					}
				} else if (Locale.ENGLISH.getLanguage().equals(language) && !StringUtil.isEmpty(locale.getCountry())) {
					locale = Locale.ENGLISH;
				}
			}
		}

		locale = locale == null ? IWMainApplication.getDefaultIWMainApplication().getDefaultLocale() : locale;
		iwc.setCurrentLocale(locale);
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		initializeContext(request, response);
		super.doFilter(request, response, chain);
	}

}