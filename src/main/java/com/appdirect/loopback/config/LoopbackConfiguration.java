package com.appdirect.loopback.config;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import com.appdirect.loopback.config.model.RequestSelector;
import com.appdirect.loopback.config.model.ResponseDelayConfiguration;

@Data
@Builder
public class LoopbackConfiguration {
	private final String name;
	private final int port;
	private final String templatePath;
	private final ResponseDelayConfiguration responseDelayConfiguration;
	private final boolean isSSL;    // Not supported
	@Singular
	private List<RequestSelector> selectors;
}
