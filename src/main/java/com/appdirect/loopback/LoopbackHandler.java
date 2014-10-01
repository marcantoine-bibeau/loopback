package com.appdirect.loopback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import com.appdirect.loopback.config.LoopbackConfiguration;
import com.appdirect.loopback.config.RequestSelector;

@Slf4j
@Data @EqualsAndHashCode(callSuper = true)
public class LoopbackHandler extends AbstractHandler {
	private final LoopbackConfiguration loopbackConfiguration;
	private final VelocityEngine velocityEngine;
	
	private Pattern httpStatusResponseLinePattern = Pattern.compile("^HTTP/1.1\\s(\\w{3})\\s.*");

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
					log.trace(loopbackConfiguration.getName() + ": Trying to match url: {}", completeRequestUrl);
					matcher = selector.getRequestMatcher().getMatcher().matcher(completeRequestUrl);
					break;
				case BODY:
					log.trace(loopbackConfiguration.getName() + ": Trying to match body.");
					String body = IOUtils.toString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8.name());
					matcher = selector.getRequestMatcher().getMatcher().matcher(body);
					break;
				case HEADERS:
					log.trace(loopbackConfiguration.getName() + ": Trying to match headers.");
					// TODO ...
					break;
					
			}

			if (matcher.find()) {
				requestSelectorUsed = selector;
				log.trace(loopbackConfiguration.getName() + ": Request matched with: {}", requestSelectorUsed.getRequestMatcher().getMatcher().toString());
				break;
			}
		}

		if (requestSelectorUsed == null) {
			request.setHandled(true);
			httpServletResponse.sendError(HttpStatus.NOT_FOUND_404);
			return;
		}

		VelocityContext context = new VelocityContext();
		context.put("name", "World");
		request.setHandled(true);
		fillResponse(requestSelectorUsed, context, httpServletResponse);
	}

	//TODO: create VelocityWriter to fill directly the HttpServletResponse
	private void fillResponse(RequestSelector requestSelectorUsed, VelocityContext context, HttpServletResponse httpServletResponse) throws IOException {
		Writer stringWriter = new StringWriter();
		Template template = velocityEngine.getTemplate(loopbackConfiguration.getTemplatePath() + requestSelectorUsed.getTemplate(), StandardCharsets.UTF_8.name());
		velocityEngine.getTemplate(loopbackConfiguration.getTemplatePath() + requestSelectorUsed.getTemplate(), StandardCharsets.UTF_8.name()).merge(context, stringWriter);

		BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
		String line = reader.readLine();
		Matcher matcher = httpStatusResponseLinePattern.matcher(line);
		
		//TODO: Validate in constructor
		if (!matcher.find()) {
			log.error("Invalid http status line response in template: {}", template.getName());
			httpServletResponse.sendError(500, "Invalid template");
			return;
		}
		httpServletResponse.setStatus(Integer.parseInt(matcher.group(1)));

		while (!(line = reader.readLine()).equals("")) {
			String[] header= line.split(":");
			if (header.length != 2) {
				log.error("Invalid http header(s) response in template: {}", template.getName());
				httpServletResponse.sendError(500, "Invalid template");
			}
			httpServletResponse.addHeader(header[0], header[1]);
		}

		while ((line = reader.readLine()) != null) {
			httpServletResponse.getWriter().write(line);
		}
	}
}
