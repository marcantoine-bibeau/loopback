package com.appdirect.loopback.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.eclipse.jetty.http.HttpMethod;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestCallback {
	private HttpMethod method;
	private String path;
	private String template;
	private String host;
	private int port;
}
