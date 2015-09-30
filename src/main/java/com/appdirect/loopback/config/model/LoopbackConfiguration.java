package com.appdirect.loopback.config.model;

import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class LoopbackConfiguration {
	private final String name;
	private final int port;
	private final Optional<Integer> securePort;
	private final String templatePath;
	private final DelayConfiguration delayConfiguration;
	private final Optional<OAuthConfiguration> oAuthConfiguration;
	@Singular
	private List<RequestSelector> selectors;
}
