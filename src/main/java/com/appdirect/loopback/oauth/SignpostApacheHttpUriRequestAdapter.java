package com.appdirect.loopback.oauth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import lombok.Data;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;

import com.google.common.collect.Maps;
import oauth.signpost.http.HttpRequest;

@Data
public class SignpostApacheHttpUriRequestAdapter implements HttpRequest {
	private final HttpUriRequest httpUriRequest;

	@Override
	public String getMethod() {
		return httpUriRequest.getMethod();
	}

	@Override
	public String getRequestUrl() {
		return httpUriRequest.getURI().toString();
	}

	@Override
	public void setRequestUrl(String url) {
		throw new IllegalStateException("Not supported...");
	}

	@Override
	public void setHeader(String name, String value) {
		httpUriRequest.setHeader(name, value);
	}

	@Override
	public String getHeader(String name) {
		Header header = httpUriRequest.getFirstHeader(name);
		return header == null ? null : httpUriRequest.getFirstHeader(name).getValue();
	}

	@Override
	public Map<String, String> getAllHeaders() {
		Map<String, String> headers = Maps.newHashMap();
		for (Header header : httpUriRequest.getAllHeaders()) {
			headers.put(header.getName(), header.getValue());
		}
		return headers;
	}

	@Override
	public InputStream getMessagePayload() throws IOException {
		throw new IllegalStateException("Not supported...");
	}

	@Override
	public String getContentType() {
		return getHeader("Content-Type");
	}

	@Override
	public Object unwrap() {
		return httpUriRequest;
	}
}
