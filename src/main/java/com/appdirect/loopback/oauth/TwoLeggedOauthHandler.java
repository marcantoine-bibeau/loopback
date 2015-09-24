package com.appdirect.loopback.oauth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;

import com.appdirect.loopback.config.model.TwoLeggedOauthConfiguration;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;
import com.sun.jersey.oauth.signature.OAuthSignature;
import com.sun.jersey.oauth.signature.OAuthSignatureException;


@Slf4j
@AllArgsConstructor
public class TwoLeggedOauthHandler extends ContextHandler {
	private final TwoLeggedOauthConfiguration twoLeggedOauthConfiguration;

	@Override
	public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		super.doHandle(target, baseRequest, request, response);
		log.info("Validating 2-Legged Oauth request signature");

		JettyOAuthRequest oAuthRequest = new JettyOAuthRequest(request);
		OAuthParameters oAuthParameters = new OAuthParameters();
		oAuthParameters.readRequest(oAuthRequest);

		OAuthSecrets secrets = new OAuthSecrets().consumerSecret(twoLeggedOauthConfiguration.getSecret());

		try {
			if (!OAuthSignature.verify(oAuthRequest, oAuthParameters, secrets)) {
				throw new OAuthSignatureException("Oauth signature verification failed.");
			}
			log.info("2-Legged Oauth request signature validated.");
		} catch (OAuthSignatureException e) {
			baseRequest.setHandled(true);
			response.sendError(401, "Not Authorized.");
		}
	}
}
