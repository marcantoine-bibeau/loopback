package com.appdirect.loopback.oauth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;

import com.appdirect.loopback.config.model.OAuthConfiguration;
import com.sun.jersey.oauth.signature.OAuthSignatureException;


@Slf4j
@AllArgsConstructor
public class OAuthHandler extends ContextHandler {
	private final OAuthConfiguration oAuthConfiguration;
	private final OAuthSignatureService oAuthSignatureService = OAuthSignatureService.getInstance();

	@Override
	public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		super.doHandle(target, baseRequest, request, response);

		try {
			oAuthSignatureService.validateSignature(request, oAuthConfiguration);
		} catch (OAuthSignatureException e) {
			baseRequest.setHandled(true);
			log.error("Failure validating 2-Legged Oauth request signature.", e);
			response.sendError(401, "Not Authorized.");
		}
	}
}
