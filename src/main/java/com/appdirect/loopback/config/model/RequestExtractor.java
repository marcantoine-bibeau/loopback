package com.appdirect.loopback.config.model;

import java.util.regex.Pattern;

import lombok.Data;

@Data
public class RequestExtractor {
	private Scope scope;
	private Pattern pattern;
}
