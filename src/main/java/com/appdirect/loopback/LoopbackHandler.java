package com.appdirect.loopback;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.appdirect.loopback.config.model.LoopbackConfiguration;
import com.appdirect.loopback.config.model.RequestCallback;
import com.appdirect.loopback.config.model.RequestSelector;
import com.appdirect.loopback.config.model.Scope;

@Slf4j
@Data
@EqualsAndHashCode( callSuper = true )
public class LoopbackHandler extends AbstractHandler {
	public static final String CRLF = "\r\n";
	public static final String HEADER_DELIMITER = ":";
	private static final Pattern httpStatusResponseLinePattern = Pattern.compile("^HTTP/1.1\\s(\\w{3})\\s.*");

	private final LoopbackConfiguration loopbackConfiguration;
	private final VelocityEngine velocityEngine;
	private Random random = new Random();

	public LoopbackHandler(LoopbackConfiguration loopbackConfiguration) {
		this.loopbackConfiguration = loopbackConfiguration;
		this.velocityEngine = new VelocityEngine();
		this.velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		this.velocityEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		this.velocityEngine.init();
	}

	@Override
	public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
		request.setHandled(true);

		Optional<RequestSelector> requestSelector = findSelector(httpServletRequest);
		if (!requestSelector.isPresent()) {
			httpServletResponse.sendError(HttpStatus.NOT_FOUND_404);
			return;
		}

		processRequest(s, requestSelector.get(), httpServletRequest, httpServletResponse);
	}

	private void processRequest(String s, RequestSelector requestSelector, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
		VelocityContext velocityContext = new VelocityContext();
		buildTemplateResponse(httpServletRequest, requestSelector, velocityContext);
		sendResponse(requestSelector.getTemplate(), velocityContext, httpServletResponse);
		requestSelector.getRequestCallback().ifPresent(requestCallback -> executeRequestCallback(requestCallback, velocityContext));
	}

	private void executeRequestCallback(RequestCallback requestCallback, VelocityContext velocityContext) {
		if (requestCallback == null) {
			return;
		}

		try {
			log.trace("Executing Request Callback " + requestCallback);
			HttpUriRequest request = createHttpRequest(requestCallback, velocityContext);

			CloseableHttpClient httpClient = HttpClients.createDefault();
			CloseableHttpResponse response = httpClient.execute(request);

			if (!HttpStatus.isSuccess(response.getStatusLine().getStatusCode())) {
				log.error("Error sending request." + response);
			}
			log.trace(response.toString());
		} catch (Exception e) {
			log.error("Unable to send request.", e);
		}

	}

	private Optional<RequestSelector> findSelector(HttpServletRequest httpServletRequest) throws IOException {
		for (RequestSelector selector : loopbackConfiguration.getSelectors()) {
			Matcher selectorMatcher = getMatcher(httpServletRequest, selector.getRequestMatcher().getPattern(), selector.getRequestMatcher().getScope());
			if (selectorMatcher.find()) {
				log.trace("[" + loopbackConfiguration.getName() + "]" + ": Request matched with: {}", selector.getRequestMatcher().getPattern().toString());
				return Optional.of(selector);
			}
		}
		return Optional.empty();
	}

	private void buildTemplateResponse(HttpServletRequest httpServletRequest, RequestSelector requestSelector, VelocityContext velocityContext) throws IOException {
		if (!requestSelector.getRequestExtractor().isPresent()) {
			return;
		}

		Matcher extractorMatcher = getMatcher(httpServletRequest, requestSelector.getRequestExtractor().get().getPattern(), requestSelector.getRequestExtractor().get().getScope());
		if (extractorMatcher.find()) {
			String[] groups = new String[extractorMatcher.groupCount()];
			for (int i = 0; i < groups.length; i++) {
				groups[i] = extractorMatcher.group(i + 1);
			}
			velocityContext.put("groups", groups);
		}
	}

	private Matcher getMatcher(HttpServletRequest httpServletRequest, Pattern pattern, Scope scope) throws IOException {
		switch (scope) {
			case URL:
				String completeRequestUrl = getFullUrl(httpServletRequest);
				log.trace("[" + loopbackConfiguration.getName() + "]" + "[" + pattern.toString() + "]" + ": Trying to match url: {}", completeRequestUrl);
				return pattern.matcher(completeRequestUrl);
			case BODY:
				log.trace("[" + loopbackConfiguration.getName() + "]" + "[" + pattern.toString() + "]" + ": Trying to match body.");
				String body = IOUtils.toString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8.name());
				return pattern.matcher(body);
			default:
				throw new IllegalStateException("Unsupported scope:  " + scope);
		}
	}

	private String getFullUrl(HttpServletRequest httpServletRequest) {
		StringBuilder sb = new StringBuilder(httpServletRequest.getMethod());
		if (StringUtils.isNotEmpty(httpServletRequest.getPathInfo())) {
			sb.append(" ").append(httpServletRequest.getPathInfo());
		} else {
			sb.append(" /");
		}
		if (StringUtils.isNotEmpty(httpServletRequest.getQueryString())) {
			sb.append("?").append(httpServletRequest.getQueryString());
		}
		return sb.toString();
	}

	private void sendResponse(String templateName, VelocityContext context, HttpServletResponse httpServletResponse) throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(getMergedTemplate(templateName, context)));
		String line = reader.readLine();
		Matcher matcher = httpStatusResponseLinePattern.matcher(line);

		if (!matcher.find()) {
			log.error("Invalid http status line response in template: {}", templateName);
			httpServletResponse.sendError(500, "Invalid template");
			return;
		}

		delayResponseIfRequired();

		httpServletResponse.setStatus(Integer.parseInt(matcher.group(1)));

		while (!(line = reader.readLine()).equals("")) {
			String[] header = line.split(":");
			if (header.length != 2) {
				log.error("Invalid http header(s) response in template: {}", templateName);
				httpServletResponse.sendError(500, "Invalid template");
			}
			httpServletResponse.addHeader(header[0], header[1]);
		}

		while ((line = reader.readLine()) != null) {
			httpServletResponse.getWriter().write(line);
		}
	}

	private void delayResponseIfRequired() {
		loopbackConfiguration.getDelayConfiguration().ifPresent(delayConfiguration -> {
			int delay = random.nextInt(delayConfiguration.getMaxDelayMs() - delayConfiguration.getMinDelayMs()) + delayConfiguration.getMinDelayMs();
			try {
				log.info("Delaying response for {}ms", delay);
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				log.error("Really???", e);
			}
		});
	}

	private String getMergedTemplate(String templateName, VelocityContext context) {
		StringWriter stringWriter = new StringWriter();
		Template template = velocityEngine.getTemplate(loopbackConfiguration.getTemplatePath() + templateName, StandardCharsets.UTF_8.name());
		template.merge(context, stringWriter);
		return stringWriter.toString();
	}

	private HttpUriRequest createHttpRequest(RequestCallback requestCallback, VelocityContext velocityContext) throws IOException {
		HttpUriRequest request;
		switch (requestCallback.getMethod()) {
			case GET:
				request = new HttpGet("http://" + requestCallback.getHost() + ":" + requestCallback.getPort() + requestCallback.getPath());
				break;
			case POST:
				request = new HttpPost("http://" + requestCallback.getHost() + ":" + requestCallback.getPort() + requestCallback.getPath());
				break;
			case PUT:
				request = new HttpPut("http://" + requestCallback.getHost() + ":" + requestCallback.getPort() + requestCallback.getPath());
				break;
			default:
				throw new RuntimeException("Unsupported method " + requestCallback.getMethod());
		}

		if (StringUtils.isEmpty(requestCallback.getTemplate())) {
			return request;
		}

		BufferedReader entityReader = new BufferedReader(new StringReader(getMergedTemplate(requestCallback.getTemplate(), velocityContext)));
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		boolean headerDone = false;
		String line;
		while ((line = entityReader.readLine()) != null) {
			if (line.equals(CRLF) || line.equals(StringUtils.LF) || line.equals(StringUtils.EMPTY)) {
				headerDone = true;
			}

			if (!headerDone) {
				int delimiterIndex = StringUtils.indexOf(line, HEADER_DELIMITER);
				if (delimiterIndex <= 0) {
					throw new IllegalArgumentException("Invalid header format. " + line);
				}
				request.addHeader(StringUtils.left(line, delimiterIndex), StringUtils.right(line, delimiterIndex));
			} else {
				byteArrayOutputStream.write(line.getBytes(StandardCharsets.UTF_8.name()));
			}
		}

		if (request instanceof HttpEntityEnclosingRequestBase) {
			((HttpEntityEnclosingRequestBase) request).setEntity(new StringEntity(byteArrayOutputStream.toString(StandardCharsets.UTF_8.name())));
		}
		return request;
	}
}
