package com.appdirect.loopback.config;

public class IllegalConfigurationException extends Exception {
	public IllegalConfigurationException() {
	}

	public IllegalConfigurationException(String message) {
		super(message);
	}

	public IllegalConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public IllegalConfigurationException(Throwable cause) {
		super(cause);
	}

	public IllegalConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
