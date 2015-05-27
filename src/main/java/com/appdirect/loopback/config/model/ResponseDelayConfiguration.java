package com.appdirect.loopback.config.model;

import lombok.Data;

@Data
public class ResponseDelayConfiguration {
	private final long minDelayMs;
	private final long maxDelayMs;
}
