package com.appdirect.loopback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.appdirect.loopback.oauth.SignpostApacheHttpUriRequestAdapter;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

public class DummyEventTest {
	public static void main(String[] args) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		doTest1(httpclient);
	}

	private static CloseableHttpClient doTest1(CloseableHttpClient httpclient) throws IOException, OAuthCommunicationException, OAuthExpectationFailedException, OAuthMessageSignerException {
		System.out.println("======   TEST #1   =========");
		HttpGet httpget = new HttpGet("http://localhost:8011/events?token=abcdefg");
		OAuthConsumer oauthConsumer = new DefaultOAuthConsumer("aaaa-492", "35IUXOUZgHRS03Vm");
		oauthConsumer.sign(new SignpostApacheHttpUriRequestAdapter(httpget));

		CloseableHttpResponse response = httpclient.execute(httpget);
		System.out.println(response.getStatusLine());
		Header[] headers = response.getAllHeaders();
		for (Header header : headers) {
			System.out.println(header.getName() + " : " + header.getValue());
		}
		System.out.println();
		System.out.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name()));
		return httpclient;
	}

}
