package com.appdirect.loopback.config;

import java.util.regex.Pattern;

import lombok.Data;

@Data
public class RequestExtractor {
	private Scope scope;
	private Pattern extractor;
}
