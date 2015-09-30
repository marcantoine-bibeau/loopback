package com.appdirect.loopback.config.model;

import lombok.Data;

@Data
public class OAuthConfiguration {
	private final String consumerId;
	private final String secret;
}
