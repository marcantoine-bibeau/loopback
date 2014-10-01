package com.appdirect.loopback.config;

import lombok.Data;

@Data
public class RequestSelector {
	private RequestMatcher requestMatcher;
	private RequestExtractor requestExtractor;
	private String template;
}
