package com.appdirect.loopback.config.model;

import lombok.Data;

@Data
public class TwoLeggedOauthConfiguration {
	private final String clientId;
	private final String secret;
}
