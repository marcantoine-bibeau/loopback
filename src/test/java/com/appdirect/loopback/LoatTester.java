package com.appdirect.loopback;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class LoatTester {

	public static void main(String[] args) throws Exception {
		HttpBlaster httpBlaster = new HttpBlaster();
		httpBlaster.start();
	}

	private static class HttpBlaster {
		final AtomicInteger mosiVerySmallCount = new AtomicInteger(0);
		final AtomicInteger comcastBigCount = new AtomicInteger(0);
		final HttpGet httpGetComcastBig = new HttpGet("http://localhost:8003/path/test/a");
		final HttpGet httpGetMosiSmall = new HttpGet("http://localhost:8002/path/test1/Name");

		public void start() throws Exception {
			Thread t1 = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while (true) {
							int totalCount = comcastBigCount.get();
							comcastBigCount.set(0);
							totalCount = totalCount + mosiVerySmallCount.get();
							mosiVerySmallCount.set(0);
							System.out.println(totalCount + "  Request per second.");
							Thread.sleep(1000L);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}, "Thread-1");

			Thread comcastBig = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while (true) {
							CloseableHttpClient httpclient = HttpClients.createDefault();
							HttpResponse response = httpclient.execute(httpGetComcastBig);
							//System.out.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name()));
							comcastBigCount.getAndIncrement();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, "Thread-2");

			Thread mosi = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while (true) {
							CloseableHttpClient httpclient = HttpClients.createDefault();
							HttpResponse response = httpclient.execute(httpGetMosiSmall);
							//System.out.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name()));
							mosiVerySmallCount.getAndIncrement();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, "Thread-2");

			t1.start();
			comcastBig.start();
			mosi.start();
		}
	}
}
