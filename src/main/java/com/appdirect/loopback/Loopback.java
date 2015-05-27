package com.appdirect.loopback;

import java.lang.management.ManagementFactory;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;

import com.appdirect.loopback.config.LoopbackConfigurationReader;
import com.appdirect.loopback.config.model.LoopbackConfiguration;
import com.google.common.collect.Maps;

@Slf4j
public class Loopback {
	private final Map<String, Server> servers = Maps.newHashMap();

	public static void main(String[] args) throws Exception {
		Loopback loopback = new Loopback();
		loopback.init();
		loopback.start();
	}

	public void init() throws Exception {
		Map<String, LoopbackConfiguration> loopbackConfig = new LoopbackConfigurationReader().loadConfiguration();
		if (loopbackConfig.isEmpty()) {
			throw new IllegalStateException("No configuration successfully loaded.");
		}

		loopbackConfig.entrySet().stream().forEach(config -> {
			log.info("Initializing loopback {}", config.getKey());
			try {
				Server server = createServer(new LoopbackHandler(config.getValue()), config.getValue().getPort());
				servers.put(config.getKey(), server);
			} catch (Exception e) {
				log.error("Unable to create loopback " + config.getKey(), e);
			}
		});
	}

	private Server createServer(LoopbackHandler handler, int port) throws Exception {
		Server server = new Server(port);
		server.setHandler(handler);

		// Setup JMX
		MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
		server.addBean(mbContainer);
		/*
		JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1098/jmxrmi");
		ConnectorServer connector = new ConnectorServer(jmxServiceURL, "org.eclipse.jetty.jmx:name=rmiconnectorserver");
		server.addManaged(connector);
		*/

		return server;
	}

	public void start() throws Exception {
		if (servers.isEmpty()) {
			log.error("No server to start...");
			throw new IllegalStateException("No loopback to start.");
		}

		servers.entrySet().stream().forEach(server -> {
			try {
				log.info("Starting [ {} ] loopback...", server.getKey());
				server.getValue().start();
				log.info("[ {} ] started!", server.getKey());
			} catch (Exception e) {
				log.error("Unable to start loopback [ " + server.getKey() + "]", e);
			}
		});

		// Probably a better way
		while (true) ;
	}
}
