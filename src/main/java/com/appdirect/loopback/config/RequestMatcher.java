package com.appdirect.loopback.config;

import java.util.regex.Pattern;

import lombok.Data;

@Data
public class RequestMatcher {
	private Scope scope;
	private Pattern matcher;
}
