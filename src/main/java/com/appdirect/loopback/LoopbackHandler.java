package com.appdirect.loopback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;

import com.appdirect.loopback.config.model.DelayConfiguration;
import com.appdirect.loopback.config.model.LoopbackConfiguration;
import com.appdirect.loopback.config.model.OAuthConfiguration;
import com.appdirect.loopback.config.model.RequestSelector;
import com.appdirect.loopback.config.model.Scope;
import com.appdirect.loopback.oauth.OAuthSignatureService;
import com.sun.jersey.oauth.signature.OAuthSignatureException;

@Slf4j
public class LoopbackHandler extends ContextHandler {
	private static final String EXTRACTED_VALUES = "extracted";
	private static final Pattern httpStatusResponseLinePattern = Pattern.compile("^HTTP/1.1\\s(\\w{3})\\s.*");

	private final LoopbackConfiguration loopbackConfiguration;
	private final OAuthSignatureService oAuthSignatureService = OAuthSignatureService.getInstance();
	private SecureRandom random = new SecureRandom();

	public LoopbackHandler(LoopbackConfiguration loopbackConfiguration) {
		this.loopbackConfiguration = loopbackConfiguration;
	}

	@Override
	public void doHandle(String s, Request baseRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
		baseRequest.setHandled(true);
		Optional<RequestSelector> requestSelector = findSelector(httpServletRequest);
		if (!requestSelector.isPresent()) {
			httpServletResponse.sendError(HttpStatus.NOT_FOUND_404);
			return;
		}

		RequestSelector selector = requestSelector.get();

		try {
			validateOAuth(httpServletRequest, selector.getOAuthConfiguration());
		} catch (OAuthSignatureException e) {
			httpServletResponse.sendError(HttpStatus.UNAUTHORIZED_401);
		}

		VelocityContext velocityContext = buildVelocityContext(httpServletRequest, selector);

		selector.getRequestCallback().ifPresent(config -> RequestCallbackExecutor.getInstance().schedule(config, loopbackConfiguration.getTemplatePath(), new VelocityContext(velocityContext), selector.getOAuthConfiguration()));
		sendResponse(selector.getTemplate(), velocityContext, httpServletResponse);
	}

	private void validateOAuth(HttpServletRequest httpServletRequest, Optional<OAuthConfiguration> oAuthConfiguration) throws OAuthSignatureException {
		if (oAuthConfiguration.isPresent()) {
			oAuthSignatureService.validateSignature(httpServletRequest, oAuthConfiguration.get());
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

	private VelocityContext buildVelocityContext(HttpServletRequest httpServletRequest, RequestSelector requestSelector) throws IOException {
		VelocityContext context = new VelocityContext();
		if (StringUtils.isNotEmpty(httpServletRequest.getHeader(LoopbackUtils.HOST))) {
			context.put(LoopbackUtils.HOST, httpServletRequest.getHeader(LoopbackUtils.HOST));
		}

		if (!requestSelector.getRequestExtractor().isPresent()) {
			return context;
		}

		Matcher extractorMatcher = getMatcher(httpServletRequest, requestSelector.getRequestExtractor().get().getPattern(), requestSelector.getRequestExtractor().get().getScope());
		pupulateVelocityContext(extractorMatcher, context);
		return context;
	}

	private void pupulateVelocityContext(Matcher extractorMatcher, VelocityContext velocityContext) {
		if (extractorMatcher.find()) {
			String[] extreacted = new String[extractorMatcher.groupCount()];
			for (int i = 0; i < extreacted.length; i++) {
				extreacted[i] = extractorMatcher.group(i + 1);
			}
			velocityContext.put(EXTRACTED_VALUES, extreacted);
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
		log.info("Sending response with template {}", templateName);
		BufferedReader reader = new BufferedReader(new StringReader(LoopbackUtils.getMergedTemplate(loopbackConfiguration.getTemplatePath() + templateName, context)));
		String line = reader.readLine();

		Matcher matcher = httpStatusResponseLinePattern.matcher(line);
		if (!matcher.find()) {
			log.error("Invalid http status line response in template: {}", templateName);
			httpServletResponse.sendError(500, "Invalid template");
			return;
		}

		delayIfRequired(loopbackConfiguration.getDelayConfiguration());
		httpServletResponse.setStatus(Integer.parseInt(matcher.group(1)));


		while ((line = reader.readLine()) != null && !line.equals("")) {
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
		log.info("Response sent.");
	}

	private void delayIfRequired(DelayConfiguration delayConfiguration) {
		int delay = delayConfiguration.getMaxDelayMs() == delayConfiguration.getMinDelayMs()
				? delayConfiguration.getMaxDelayMs()
				: random.nextInt(delayConfiguration.getMaxDelayMs() - delayConfiguration.getMinDelayMs()) + delayConfiguration.getMinDelayMs();
		try {
			log.info("Delaying for {}ms", delay);
			Thread.sleep(delay);    //TODO: Not good...
		} catch (InterruptedException e) {
			log.error("Really???", e);
		}
	}
}
