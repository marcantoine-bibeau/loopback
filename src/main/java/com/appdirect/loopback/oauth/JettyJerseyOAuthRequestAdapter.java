package com.appdirect.loopback.oauth;

import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import com.google.common.collect.Lists;
import com.sun.jersey.oauth.signature.OAuthRequest;

@Data
public class JettyJerseyOAuthRequestAdapter implements OAuthRequest {
	private final HttpServletRequest httpServletRequest;

	public static String getUrl(HttpServletRequest req) {
		String reqUrl = req.getRequestURL().toString();
		String queryString = req.getQueryString();
		if (queryString != null) {
			reqUrl += "?" + queryString;
		}
		return reqUrl;
	}

	@Override
	public String getRequestMethod() {
		return httpServletRequest.getMethod();
	}

	@Override
	public URL getRequestURL() {
		try {
			URIBuilder uriBuilder = new URIBuilder(getUrl(httpServletRequest));
			return uriBuilder.build().toURL();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> getParameterNames() {
		return httpServletRequest.getParameterMap().keySet();
	}

	@Override
	public List<String> getParameterValues(String name) {
		return Lists.newArrayList(httpServletRequest.getParameterValues(name));

	}

	@Override
	public List<String> getHeaderValues(String name) {
		String headerValue = httpServletRequest.getHeader(name);
		if (headerValue == null || StringUtils.isEmpty(headerValue)) {
			return Lists.newArrayList();
		}

		return Lists.newArrayList(headerValue);
	}

	@Override
	public void addHeaderValue(String name, String value) throws IllegalStateException {
		throw new IllegalStateException("Not implemented");
	}
}
