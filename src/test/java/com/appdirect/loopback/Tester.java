package com.appdirect.loopback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class Tester {
	public static void main(String[] args) throws Exception {
		//CloseableHttpClient httpclient = HttpClients.createDefault();
		//doTest1(httpclient);
		//doTest2(httpclient);
		//doTest3(httpclient);
		doTest4();
	}

	private static CloseableHttpClient doTest1(CloseableHttpClient httpclient) throws IOException {
		System.out.println("======   TEST #1   =========");
		HttpGet httpget = new HttpGet("http://localhost:8003/path/test/a");
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

	private static CloseableHttpClient doTest2(CloseableHttpClient httpclient) throws IOException {
		System.out.println("======   TEST #2   =========");
		HttpGet httpget = new HttpGet("http://localhost:8003/path/test1/BillyJoe");
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

	private static CloseableHttpClient doTest3(CloseableHttpClient httpclient) throws IOException {
		System.out.println("======   TEST #3   =========");
		HttpGet httpget = new HttpGet("http://localhost:8002/path/test/a");
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

	private static CloseableHttpClient doTest4() throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		Server listener = new Server(80);

		System.out.println("======   TEST #4   =========");
		HttpGet httpget = new HttpGet("http://localhost:8003/login/openid?openid.ns=http://specs.openid.net/auth/2.0&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select&openid.identity=http://specs.openid.net/auth/2.0/identifier_select&openid.return_to=https://test1.marketplace-test.ibmcloud.com/openid/partner/finish&openid.realm=https://test1.marketplace-test.ibmcloud.com&openid.assoc_handle=77891716c55229df&openid.mode=checkid_setup&openid.ns.ext1=http://openid.net/srv/ax/1.0&openid.ext1.mode=fetch_request&openid.ext1.type.email=http://axschema.org/contact/email&openid.ext1.type.verifiedEmail=http://www.ibm.com/axschema/idaas/verified_email&openid.ext1.type.firstName=http://axschema.org/namePerson/first&openid.ext1.type.lastName=http://axschema.org/namePerson/last&openid.ext1.type.organization=http://www.ibm.com/axschema/webidentity/organization&openid.ext1.type.billingType=http://www.ibm.com/axschema/marketplace/billingtype&openid.ext1.required=email,verifiedEmail,firstName,lastName,organization,billingType");
		CloseableHttpResponse response = httpclient.execute(httpget);
		System.out.println(response.getStatusLine());
		Header[] headers = response.getAllHeaders();
		for (Header header : headers) {
			System.out.println(header.getName() + " : " + header.getValue());
		}
		System.out.println();
		System.out.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name()));

		listener.setHandler(new AbstractHandler() {
			@Override
			public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
				System.out.println("Received: " + httpServletRequest);
			}
		});
		while (true) ;

	}
}
