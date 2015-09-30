package com.appdirect.loopback.config.model;

import lombok.Data;

@Data
public class RequestCallback {
	private final String template;
	private final String host;
	private final int delay;
}
