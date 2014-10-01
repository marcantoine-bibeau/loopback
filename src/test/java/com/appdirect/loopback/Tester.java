package com.appdirect.loopback;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class Tester {
	public static void main(String[] args) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet("http://localhost:8003/path/test");
		CloseableHttpResponse response = httpclient.execute(httpget);
		System.out.println(response.getStatusLine());
		Header[] headers = response.getAllHeaders();
		for (Header header : headers) {
			System.out.println(header.getName() + " : " + header.getValue());
		}
		System.out.println();
		System.out.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name()));

		httpget = new HttpGet("http://localhost:8003/path/test1");
		response = httpclient.execute(httpget);
		System.out.println(response.getStatusLine());
		headers = response.getAllHeaders();
		for (Header header : headers) {
			System.out.println(header.getName() + " : " + header.getValue());
		}
		System.out.println();
		System.out.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name()));

		// httpclient.close();
	}
}
