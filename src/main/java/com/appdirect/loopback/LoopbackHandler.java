package com.appdirect.loopback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.regex.Matcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Data;
import lombok.extern.java.Log;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.appdirect.loopback.config.LoopbackConfiguration;
import com.appdirect.loopback.config.RequestSelector;

@Log
@Data
public class LoopbackHandler extends AbstractHandler {
	private final LoopbackConfiguration loopbackConfiguration;
	private final VelocityEngine velocityEngine;

	public LoopbackHandler(LoopbackConfiguration loopbackConfiguration) {
		this.loopbackConfiguration = loopbackConfiguration;
		this.velocityEngine = new VelocityEngine();
		this.velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		this.velocityEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		this.velocityEngine.init();
	}

	@Override
	public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
		Matcher matcher = null;
		RequestSelector requestSelectorUsed = null;

		for (RequestSelector selector : loopbackConfiguration.getSelectors()) {
			switch (selector.getRequestMatcher().getScope()) {
				case URL:
					String completeRequestUrl = httpServletRequest.getMethod() + " " + httpServletRequest.getPathInfo() + "/" + httpServletRequest.getQueryString();
					log.log(Level.INFO, loopbackConfiguration.getName() + ": Trying to match url: {}", completeRequestUrl);
					matcher = selector.getRequestMatcher().getMatcher().matcher(completeRequestUrl);
					break;
				case BODY:
					log.log(Level.INFO, loopbackConfiguration.getName() + ": Trying to match body.");
					String body = IOUtils.toString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8.name());
					matcher = selector.getRequestMatcher().getMatcher().matcher(body);
					break;
				case HEADERS:
					log.log(Level.INFO, loopbackConfiguration.getName() + ": Trying to match headers.");
					// TODO ...
					break;
					
			}

			if (matcher.find()) {
				requestSelectorUsed = selector;
				log.log(Level.INFO, loopbackConfiguration.getName() + ": Request matched with: {}", selector.getRequestMatcher().getMatcher().toString());
				break;
			}
		}

		if (requestSelectorUsed == null) {
			request.setHandled(true);
			httpServletResponse.sendError(HttpStatus.NOT_FOUND_404);
			return;
		}

		request.setHandled(true);
		httpServletResponse.setStatus(HttpStatus.OK_200);

		VelocityContext context = new VelocityContext();
		context.put("name", "World");
		velocityEngine.getTemplate(loopbackConfiguration.getTemplatePath() + requestSelectorUsed.getTemplate(), StandardCharsets.UTF_8.name()).merge(context, httpServletResponse.getWriter());
	}
}
