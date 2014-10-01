package com.appdirect.loopback;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class LoatTester {

	public static void main(String[] args) throws Exception {
		HttpBlaster httpBlaster = new HttpBlaster();
		httpBlaster.start();

	}

	private static class HttpBlaster {
		final AtomicInteger count = new AtomicInteger(0);
		final HttpGet httpget = new HttpGet("http://localhost:8002/path/test");

		public void start() throws Exception {
			Thread t1 = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while (true) {
							System.out.println(count + " Request second.");
							count.set(0);
							Thread.sleep(1000L);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}, "Thread-1");

			Thread t2 = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while (true) {
							CloseableHttpClient httpclient = HttpClients.createDefault();
							httpclient.execute(httpget);
							count.getAndIncrement();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, "Thread-2");

			t1.start();
			t2.start();
		}
	}
}
