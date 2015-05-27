package com.appdirect.loopback.config.model;

import lombok.Data;

@Data
public class DelayConfiguration {
	private final int minDelayMs;
	private final int maxDelayMs;
}
