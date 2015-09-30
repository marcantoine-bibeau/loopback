package com.appdirect.loopback.oauth;

import javax.servlet.http.HttpServletRequest;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.methods.HttpUriRequest;

import com.appdirect.loopback.config.model.OAuthConfiguration;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;
import com.sun.jersey.oauth.signature.OAuthSignature;
import com.sun.jersey.oauth.signature.OAuthSignatureException;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

@Slf4j
public class OAuthSignatureService {
	private static OAuthSignatureService instance = new OAuthSignatureService();

	private OAuthSignatureService() {
	}

	public static OAuthSignatureService getInstance() {
		return instance;
	}

	public void validateSignature(@NonNull HttpServletRequest request, @NonNull OAuthConfiguration oAuthConfiguration) throws OAuthSignatureException {
		log.info("Validating 2-Legged Oauth request signature");

		JettyJerseyOAuthRequestAdapter oAuthRequest = new JettyJerseyOAuthRequestAdapter(request);
		OAuthParameters oAuthParameters = new OAuthParameters();
		oAuthParameters.readRequest(oAuthRequest);

		OAuthSecrets secrets = new OAuthSecrets().consumerSecret(oAuthConfiguration.getSecret());

		if (!OAuthSignature.verify(oAuthRequest, oAuthParameters, secrets)) {
			throw new OAuthSignatureException("Oauth signature verification failed.");
		}
		log.info("2-Legged Oauth request signature validated.");
	}

	public void sign(@NonNull HttpUriRequest request, @NonNull OAuthConfiguration oAuthConfiguration) throws OAuthCommunicationException, OAuthExpectationFailedException, OAuthMessageSignerException {
		DefaultOAuthConsumer consumer = new DefaultOAuthConsumer(oAuthConfiguration.getConsumerId(), oAuthConfiguration.getSecret());
		consumer.sign(new SignpostApacheHttpUriRequestAdapter(request));
	}
}
