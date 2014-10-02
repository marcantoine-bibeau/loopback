package com.appdirect.loopback;

import java.io.IOException;
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
		doTest1(httpclient);
		doTest2(httpclient);
		doTest3(httpclient);
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
}
