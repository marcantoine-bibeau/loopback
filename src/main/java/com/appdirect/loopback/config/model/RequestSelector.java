package com.appdirect.loopback.config.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestSelector {
	private RequestMatcher requestMatcher;
	private RequestExtractor requestExtractor;
	private String template;
	private RequestCallback requestCallback;
}
