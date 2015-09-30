package com.appdirect.loopback;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServer;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;

import com.appdirect.loopback.config.LoopbackConfigurationReader;
import com.appdirect.loopback.config.model.LoopbackConfiguration;
import com.appdirect.loopback.oauth.OAuthHandler;
import com.google.common.collect.Lists;

@Slf4j
public class Loopback {
	private Server server;

	public static void main(String[] args) throws Exception {
		Loopback loopback = new Loopback();
		try {
			loopback.init();
			loopback.start();
		} catch (Exception e) {
			log.error("Unable to start...", e);
			loopback.stop();
		}
	}

	public void init() throws Exception {
		Collection<LoopbackConfiguration> loopbackConfig = new LoopbackConfigurationReader().loadConfiguration();
		if (loopbackConfig.isEmpty()) {
			throw new IllegalStateException("No configuration successfully loaded.");
		}

		HandlerCollection handlers = new HandlerCollection();
		List<Connector> connectors = Lists.newArrayList();

		server = createServer();

		loopbackConfig.stream().forEach(config -> {
			connectors.add(createHttpConnector(server, config));
			handlers.addHandler(createHandler(config));
		});

		server.setConnectors(connectors.toArray(new Connector[connectors.size()]));
		server.setHandler(handlers);
	}

	private Server createServer() {
		Server server = new Server();
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
		server.addEventListener(mBeanContainer);
		//server.addBean(mBeanContainer, true);
		return server;
	}

	private Connector createHttpConnector(Server server, LoopbackConfiguration configuration) {
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		if (configuration.getSecurePort().isPresent()) {
			httpConfiguration.setSecurePort(configuration.getSecurePort().get());
			httpConfiguration.setSecureScheme("https");
		}

		ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
		httpConnector.setPort(configuration.getPort());
		httpConnector.setIdleTimeout(30000);
		httpConnector.setName(configuration.getName());
		return httpConnector;
	}

	private Handler createHandler(LoopbackConfiguration configuration) {
		HandlerCollection handlers = new HandlerCollection();
		if (configuration.getOAuthConfiguration().isPresent()) {
			ContextHandler handler = new OAuthHandler(configuration.getOAuthConfiguration().get());
			handler.setVirtualHosts(new String[]{"@" + configuration.getName()});
			handler.setContextPath("/");
			handlers.addHandler(new OAuthHandler(configuration.getOAuthConfiguration().get()));
		}

		ContextHandler handler = new LoopbackHandler(configuration);
		handler.setVirtualHosts(new String[]{"@" + configuration.getName()});
		handler.setContextPath("/");
		handlers.addHandler(handler);

		return handlers;
	}

	public void start() throws Exception {
		server.start();
		RequestCallbackExecutor.getInstance();
		server.join();
	}

	public void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}
}
