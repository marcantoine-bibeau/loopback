package com.appdirect.loopback.config;

import java.util.List;

import lombok.Data;

import com.google.common.collect.Lists;

@Data
public class LoopbackConfiguration {
	private String name;
	private int port;
	private String templatePath;
	private List<RequestSelector> selectors = Lists.newLinkedList();
	
	public void addSelector(RequestSelector requestSelector) {
		selectors.add(requestSelector);
	}
	
	private enum SelectorType {
		URL, BODY, HEADER
	}
}
