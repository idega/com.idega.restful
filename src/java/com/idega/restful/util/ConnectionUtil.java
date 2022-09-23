package com.idega.restful.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.idegaweb.IWMainApplication;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

public class ConnectionUtil {

	private static final Logger LOGGER = Logger.getLogger(ConnectionUtil.class.getName());
	private static final ConnectionUtil instance = new ConnectionUtil();

	private ConnectionUtil() {}

	public static final ConnectionUtil getInstance() {
		return instance;
	}

	private boolean isAllowedToAcceptAllCertificates(String url) {
		return !StringUtil.isEmpty(url) &&
				url.startsWith("https") &&
				IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("rest.accept_all_cert", Boolean.TRUE);
	}

	private static Client ACCEPTING_EVERYTHING_CLIENT = null;

	public String getBasicAuthorization(String username, String password) throws Exception {
		if (StringUtil.isEmpty(username) || StringUtil.isEmpty(password)) {
			return null;
		}

		byte[] bytes = Base64.getEncoder().encode(username.concat(CoreConstants.COLON).concat(password).getBytes(CoreConstants.ENCODING_UTF8));
		return "Basic ".concat(new String(bytes, CoreConstants.ENCODING_UTF8));
	}

	public String getEncodedFormData(Map<String, String> params) throws UnsupportedEncodingException {
		if (MapUtil.isEmpty(params)) {
			return null;
		}

	    StringBuilder result = new StringBuilder();
	    boolean first = true;
	    for (Map.Entry<String, String> entry: params.entrySet()) {
	        if (first) {
	            first = false;
	        } else {
	            result.append(CoreConstants.AMP);
	        }

	        result.append(URLEncoder.encode(entry.getKey(), CoreConstants.ENCODING_UTF8));
	        result.append(CoreConstants.EQ);
	        result.append(URLEncoder.encode(entry.getValue(), CoreConstants.ENCODING_UTF8));
	    }

	    return result.toString();
	}

	public Client getClient(String url) {
		return getClient(url, null);
	}

	public Client getClient(String url, DefaultClientConfig config) {
		if (isAllowedToAcceptAllCertificates(url)) {
			if (ACCEPTING_EVERYTHING_CLIENT == null) {
				//	Create a trust manager that does not validate certificate chains
				TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
					    @Override
						public X509Certificate[] getAcceptedIssuers() {
					    	return null;
					    }
					    @Override
						public void checkClientTrusted(X509Certificate[] certs, String authType) {}
					    @Override
						public void checkServerTrusted(X509Certificate[] certs, String authType) {}
					}
				};

				//	Install the all-trusting trust manager
				SSLContext sc = null;
				try {
				    sc = SSLContext.getInstance(IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty("rest.ssl_protocol", "TLSv1.2"));
				    sc.init(null, trustAllCerts, new SecureRandom());
				    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
				} catch (Exception e) {}

				config = config == null ? new DefaultClientConfig() : config;
				config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
						new HostnameVerifier() {
							@Override
							public boolean verify(String s, SSLSession sslSession) {
								return true;
							}
						}, sc
					)
				);
				ACCEPTING_EVERYTHING_CLIENT = Client.create(config);
			}
			return ACCEPTING_EVERYTHING_CLIENT;
		} else {
			return config == null ? new Client() : Client.create(config);
		}
	}

	/**
	 * Returns response from remote syste
	 *
	 * @param <D> - type of result
	 * @param uri - remote system
	 * @param length - size of request (for POST)
	 * @param type - content type
	 * @param method - type of HTTP request (GET, POST etc.)
	 * @param data - request
	 * @param headerParams - parameters for header
	 * @param pathParams - path parameters
	 * @param queryParams - query parameters
	 * @return
	 */
	public <D> ClientResponse getResponseFromREST(
			String uri,
			Long length,
			String type,
			String method,
			D data,
			List<AdvancedProperty> headerParams,
			List<AdvancedProperty> pathParams,
			AdvancedProperty... queryParams
	) {
		try {
			if (!ListUtil.isEmpty(pathParams)) {
				for (AdvancedProperty pathParam: pathParams) {
					String pattern = CoreConstants.CURLY_BRACKET_LEFT + pathParam.getName() + CoreConstants.CURLY_BRACKET_RIGHT;
					uri = StringHandler.replace(uri, pattern, pathParam.getValue().toString());
				}
			}

			if (!ArrayUtil.isEmpty(queryParams)) {
				URIUtil uriUtil = new URIUtil(uri);
				for (AdvancedProperty queryParam: queryParams) {
					String value = queryParam.getValue();
					if (StringUtil.isEmpty(value)) {
						continue;
					}

					uriUtil.setParameter(queryParam.getName(), value);
				}
				uri = uriUtil.getUri();
			}

			WebResource.Builder builder = getBackendResource(uri, length);
			if (builder == null) {
				return null;
			}

			if (!ListUtil.isEmpty(headerParams)) {
				for (AdvancedProperty headerParam: headerParams) {
					builder = builder.header(headerParam.getName(), headerParam.getValue());
				}
			}

			ClientResponse response = builder
							.type(type)
							.method(method, ClientResponse.class, data);

			return response;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error calling RESTful WS at " + uri + ", type " + type + ", method " + method + ". Header params: " + headerParams + ", path params: " +
					pathParams + ", data: " + data + ", query params: " + queryParams, e);
		}
		return null;
	}

	private final WebResource.Builder getBackendResource(String url, Long contentLength) throws Exception {
		if (StringUtil.isEmpty(url)) {
			return null;
		}

		Client client = getClient(url);
		WebResource webResource = client.resource(url);
		WebResource.Builder builder = webResource.getRequestBuilder();
		if (contentLength != null) {
			builder = webResource.header(CoreConstants.PARAMETER_CONTENT_LENGTH, String.valueOf(contentLength));
		}
		return builder;
	}

}