package com.appdirect.loopback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.common.collect.Lists;

@Slf4j
@Data @EqualsAndHashCode(callSuper = true)
public class LoopbackHandler extends AbstractHandler {
	private List<RequestMatcher> requestMatchers = Lists.newArrayList();
	private final LoopbackConfiguration loopbackConfiguration;

	public LoopbackHandler(LoopbackConfiguration loopbackConfiguration) {
		this.loopbackConfiguration = loopbackConfiguration;

		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		velocityEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		velocityEngine.init();

		// TODO: do it right
		// TODO: Order is important...
		Properties properties = new Properties();
		properties.put("request.matcher.url.1", "/path/test/.*");
		properties.put("request.extractor.1", "/path/test/.*");
		properties.put("request.template.1", "/loopbacks/comcast/templates/helloworld.vm");

		properties.put("request.matcher.body.2", "user=12345");

		for (Map.Entry<Object, Object> property : properties.entrySet()) {
			String key = (String) property.getKey();
			String value = (String) property.getValue();

			if (!key.toLowerCase().contains("request")) {
				log.trace("Ignore config: {}", key);
				continue;
			}

			RequestMatcher requestMatcher;
			if (key.contains(RequestMatcherType.URL.name().toLowerCase())) {
				requestMatcher = new RequestMatcher(RequestMatcherType.URL, null, Pattern.compile(value), velocityEngine.getTemplate("loopbacks/comcast/templates/helloworld.vm",
						StandardCharsets.UTF_8.name()));
			} else if (key.contains(RequestMatcherType.BODY.name().toLowerCase())) {
				requestMatcher = new RequestMatcher(RequestMatcherType.BODY, null, Pattern.compile(value), velocityEngine.getTemplate("loopbacks/comcast/templates/helloworld.vm",
						StandardCharsets.UTF_8.name()));
			} else {
				// throw new
				// IllegalArgumentException("Configuration key must be in format: \"request/matcher/[url|body]/X\" current is: "
				// + key);
				continue;
			}

			requestMatchers.add(requestMatcher);
			log.trace("Adding request matcher: {}", requestMatcher.toString());
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
					log.trace(loopbackConfiguration.getName() + ": Trying to match url: {}", completeRequestUrl);
					matcher = requestMatcher.getPattern().matcher(completeRequestUrl);
					break;
				case BODY:
					log.trace(loopbackConfiguration.getName() + ": Trying to match body.");
					String body = IOUtils.toString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8.name());
					matcher = requestMatcher.getPattern().matcher(body);
					break;
			}

			if (matcher.find()) {
				requestMatcherUsed = requestMatcher;
				log.trace(loopbackConfiguration.getName() + ": Request matched with: {}", requestMatcherUsed.getPattern().toString());
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

		VelocityContext context = new VelocityContext();
		context.put("name", "World");
		requestMatcherUsed.getVelocityTemplate().merge(context, httpServletResponse.getWriter());
	}

	@Data
	@AllArgsConstructor
	private static class RequestMatcher {
		private RequestMatcherType requestMatcherType;
		private Map<String, Pattern> extractor;
		private Pattern pattern;
		private Template velocityTemplate;
	}

	private enum RequestMatcherType {
		URL, BODY
	}
}
