package com.idega.restful.util;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientResponse;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.idegaweb.IWMainApplication;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;

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
				IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("rest.accept_all_cert", Boolean.FALSE);
	}

	public Client getClient(String url) {
		if (isAllowedToAcceptAllCertificates(url)) {
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
			    sc = SSLContext.getInstance("TLS");
			    sc.init(null, trustAllCerts, new SecureRandom());
			    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			} catch (Exception e) {}

			ClientBuilder clientBuilder = ClientBuilder.newBuilder();
	        try {
	            clientBuilder.sslContext(sc);
	            clientBuilder.hostnameVerifier(new HostnameVerifier() {
	                @Override
	                public boolean verify(String hostname, SSLSession session) {
	                    return true;
	                }
	            });
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        }

			return clientBuilder.build();
		} else {
			return ClientBuilder.newClient();
		}
	}

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
					uriUtil.setParameter(queryParam.getName(), queryParam.getValue().toString());
				}
				uri = uriUtil.getUri();
			}

			Builder builder = getBackendResource(uri, length);
			if (builder == null) {
				return null;
			}

			if (!ListUtil.isEmpty(headerParams)) {
				for (AdvancedProperty headerParam: headerParams) {
					builder = builder.header(headerParam.getName(), headerParam.getValue());
				}
			}

			return builder.method(method, Entity.entity(data, type), ClientResponse.class);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error calling RESTful WS at " + uri + ". Header params: " + headerParams + ", path params: " +
					pathParams + ", data: " + data + ", query params: " + queryParams, e);
		}
		return null;
	}

	private final Builder getBackendResource(String url, Long contentLength) throws Exception {
		if (StringUtil.isEmpty(url)) {
			return null;
		}

		Client client = getClient(url);
		WebTarget target = client.target(url);
		Builder builder = target.request();
		if (contentLength != null)
			builder = builder.header(CoreConstants.PARAMETER_CONTENT_LENGTH, String.valueOf(contentLength));
		return builder;
	}

}