package com.appdirect.loopback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.java.Log;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.common.collect.Lists;

@Log
@Data
public class LoopbackHandler extends AbstractHandler {
	private List<RequestMatcher> requestMatchers = Lists.newArrayList();
	private final LoopbackConfiguration loopbackConfiguration;

	public LoopbackHandler(LoopbackConfiguration loopbackConfiguration) {
		this.loopbackConfiguration = loopbackConfiguration;

		// TODO: do it right
		// TODO: Order is important...
		Properties properties = new Properties();
		properties.put("request.matcher.url.1", "/path/test/.*");
		properties.put("request.extractor.1", "/path/test/.*");

		properties.put("request.matcher.body.2", "user=12345");

		for (Map.Entry<Object, Object> property : properties.entrySet()) {
			String key = (String) property.getKey();
			String value = (String) property.getValue();

			if (!key.toLowerCase().contains("request.matcher.")) {
				log.log(Level.INFO, "Ignore config: {}", key);
				continue;
			}

			RequestMatcher requestMatcher;
			if (key.contains(RequestMatcherType.URL.name().toLowerCase())) {
				requestMatcher = new RequestMatcher(RequestMatcherType.URL, Pattern.compile(value));
			} else if (key.contains(RequestMatcherType.BODY.name().toLowerCase())) {
				requestMatcher = new RequestMatcher(RequestMatcherType.BODY, Pattern.compile(value));
			} else {
				throw new IllegalArgumentException("Configuration key must be in format: \"request/matcher/[url|body]/X\" current is: " + key);
			}
			requestMatchers.add(requestMatcher);
			log.log(Level.INFO, "Adding request matcher: {}", requestMatcher.toString());
		}
	}

	@Override
	public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
		Matcher matcher = null;
		RequestMatcher requestMatcherUsed = null;

		for (RequestMatcher requestMatcher : requestMatchers) {
			switch (requestMatcher.getRequestMatcherType()) {
				case URL:
					String completeRequestUrl = httpServletRequest.getMethod() + " " + httpServletRequest.getPathInfo() + "/" + httpServletRequest.getQueryString();
					log.log(Level.INFO, loopbackConfiguration.getName() + ": Trying to match url: {}", completeRequestUrl);
					matcher = requestMatcher.getPattern().matcher(completeRequestUrl);
					break;
				case BODY:
					log.log(Level.INFO, loopbackConfiguration.getName() + ": Trying to match body.");
					String body = IOUtils.toString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8.name());
					matcher = requestMatcher.getPattern().matcher(body);
					break;
			}

			if (matcher.find()) {
				requestMatcherUsed = requestMatcher;
				log.log(Level.INFO, loopbackConfiguration.getName() + ": Request matched with: {}", requestMatcherUsed.getPattern().toString());
				break;
			}
		}

		if (requestMatcherUsed == null) {
			request.setHandled(true);
			httpServletResponse.sendError(HttpStatus.NOT_FOUND_404);
			return;
		}

		request.setHandled(true);
		httpServletResponse.setStatus(HttpStatus.OK_200);
		httpServletResponse.getWriter().write("Now work with Velocity...");
	}

	@Data
	@AllArgsConstructor
	private static class RequestMatcher {
		private RequestMatcherType requestMatcherType;
		private Pattern pattern;
	}

	private enum RequestMatcherType {
		URL, BODY
	}
}
