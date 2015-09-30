package com.appdirect.loopback;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.velocity.VelocityContext;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;

import com.appdirect.loopback.config.model.OAuthConfiguration;
import com.appdirect.loopback.config.model.RequestCallback;
import com.appdirect.loopback.oauth.OAuthSignatureService;

@Slf4j
public class RequestCallbackExecutor {
	private static final Pattern httpRequestLinePattern = Pattern.compile("^([A-Z]+)\\s(\\p{ASCII}+)\\sHTTP/1.1$");
	private static final RequestCallbackExecutor instance = new RequestCallbackExecutor();
	private final OAuthSignatureService oAuthSignatureService = OAuthSignatureService.getInstance();
	private final Scheduler scheduler;

	private RequestCallbackExecutor() {
		scheduler = new ScheduledExecutorScheduler();
		try {
			scheduler.start();
			log.info("Scheduler started...");
		} catch (Exception e) {
			log.error("Unable to start Scheduler.", e);
		}
	}

	public static RequestCallbackExecutor getInstance() {
		return instance;
	}

	public void schedule(RequestCallback requestCallback, String templatePath, VelocityContext velocityContext, Optional<OAuthConfiguration> oAuthConfiguration) {
		log.info("Scheduling: {}", requestCallback);
		scheduler.schedule(() ->
				executeRequestCallback(requestCallback, templatePath, velocityContext, oAuthConfiguration), requestCallback.getDelay(), TimeUnit.MILLISECONDS);
	}

	private void executeRequestCallback(@NonNull RequestCallback requestCallback, @NonNull String templatePath, @NonNull VelocityContext velocityContext, Optional<OAuthConfiguration> oAuthConfiguration) {
		try {
			log.info("Executing Request Callback " + requestCallback);

			HttpUriRequest request = createHttpRequest(requestCallback, templatePath, velocityContext);

			if (oAuthConfiguration.isPresent()) {
				oAuthSignatureService.sign(request, oAuthConfiguration.get());
			}

			HttpClient httpClient = HttpClients.createDefault();
			log.info("Sending request. " + request);
			HttpResponse response = httpClient.execute(request);

			if (!HttpStatus.isSuccess(response.getStatusLine().getStatusCode())) {
				log.error("Error sending request. " + response);
			} else {
				log.info(response.toString());
			}
		} catch (Exception e) {
			log.error("Unable to send request.", e);
		}
	}

	private HttpUriRequest createHttpRequest(RequestCallback requestCallback, String templatePath, VelocityContext velocityContext) throws IOException {
		velocityContext.put(LoopbackUtils.HOST, requestCallback.getHost());

		BufferedReader requestReader = new BufferedReader(new StringReader(LoopbackUtils.getMergedTemplate(templatePath + requestCallback.getTemplate(), velocityContext)));

		String httpRequestLine = requestReader.readLine();
		Matcher requestLineMatcher = httpRequestLinePattern.matcher(httpRequestLine);
		if (!requestLineMatcher.find()) {
			throw new IllegalStateException("Invalid http request line for template " + requestCallback.getTemplate());
		}

		HttpMethod requestMethod = HttpMethod.fromString(requestLineMatcher.group(1));

		HttpUriRequest httpUriRequest;
		switch (requestMethod) {
			case GET:
				httpUriRequest = new HttpGet("http://" + requestLineMatcher.group(2));
				break;
			case POST:
				httpUriRequest = new HttpPost("http://" + requestLineMatcher.group(2));
				break;
			case PUT:
				httpUriRequest = new HttpPut("http://" + requestLineMatcher.group(2));
				break;
			case DELETE:
				httpUriRequest = new HttpDelete("http://" + requestLineMatcher.group(2));
				break;
			case HEAD:
				httpUriRequest = new HttpHead("http://" + requestLineMatcher.group(2));
				break;
			default:
				throw new RuntimeException("Unsupported method " + requestMethod);
		}

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		//byteArrayOutputStream.write(httpRequestLine.getBytes(StandardCharsets.UTF_8.name()));

		boolean headerDone = false;
		String line;
		while ((line = requestReader.readLine()) != null) {
			if (line.equals(LoopbackUtils.CRLF) || line.equals(StringUtils.LF) || line.equals(StringUtils.EMPTY)) {
				headerDone = true;
			}

			if (!headerDone) {
				int delimiterIndex = StringUtils.indexOf(line, LoopbackUtils.HEADER_DELIMITER);
				if (delimiterIndex <= 0) {
					throw new IllegalArgumentException("Invalid header format. " + line);
				}
				httpUriRequest.addHeader(StringUtils.left(line, delimiterIndex), line.trim().substring(delimiterIndex + 1).trim());
			} else {
				byteArrayOutputStream.write(line.getBytes(StandardCharsets.UTF_8.name()));
			}
		}

		if (httpUriRequest instanceof HttpEntityEnclosingRequestBase) {
			((HttpEntityEnclosingRequestBase) httpUriRequest).setEntity(new StringEntity(byteArrayOutputStream.toString(StandardCharsets.UTF_8.name())));
		}

		return httpUriRequest;
	}
}
