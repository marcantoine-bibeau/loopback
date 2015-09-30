package com.appdirect.loopback.config.model;

import java.util.Optional;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestSelector {
	private final Optional<OAuthConfiguration> oAuthConfiguration;
	private String name;
	private RequestMatcher requestMatcher;
	private Optional<RequestExtractor> requestExtractor;
	private String template;
	private Optional<RequestCallback> requestCallback;
}
