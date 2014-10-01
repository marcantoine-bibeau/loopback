package com.appdirect.loopback;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.eclipse.jetty.server.Server;

import com.google.common.collect.Maps;

@Slf4j
public class Loopback {
	private static final String CONFIGURATION_FILE = "configuration.xml";
	private static final String LOOPBACKCONFIG_KEY = "loopbackConfig";
	private static final String LOOPBACK_KEY = "loopback";
	private static final String LOOPBACK_NAME_ATTR = "[@name]";
	private static final String LOOPBACK_PORT_ATTR = "[@port]";
	
	private final Map<String, Server> servers = Maps.newHashMap();
	
	public static void main(String[] args) throws Exception {
		Loopback loopback = new Loopback();
		loopback.init();
		loopback.start();
	}

	public void init() {
		Map<String, LoopbackConfiguration> loopbackConfig = loadConfiguration();
		if (loopbackConfig != null && !loopbackConfig.isEmpty()) {
			for (Map.Entry<String, LoopbackConfiguration> loopback : loopbackConfig.entrySet()) {
				log.info("Initializing loopback {}", loopback.getKey());
				LoopbackHandler handler = new LoopbackHandler(loopback.getValue());
				Server server = new Server(loopback.getValue().getPort());
				server.setHandler(handler);
				servers.put(loopback.getKey(), server);
			}
		} else {
			log.error("No loopback configuration, cannot continue!");
		}	
	}
	
	public void start() throws Exception {
		if (servers.isEmpty()) {
			log.error("No server to start...");
			return;
		}
		
		for (Map.Entry<String, Server> server : servers.entrySet()) {
			log.info("Starting [" + server.getKey() + "] server...");
			server.getValue().start();
			log.info("[ {} ] started!", server.getKey());
		}
		// Probably a better way
		while (true)
			;
	}
	
	private Map<String, LoopbackConfiguration> loadConfiguration() {
		XMLConfiguration config = new XMLConfiguration();
		try {
			config.load(Loopback.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE));
			List<HierarchicalConfiguration> loopbackConfigs = config.configurationsAt(LOOPBACKCONFIG_KEY);
			if (loopbackConfigs != null && !loopbackConfigs.isEmpty()) {
				Map<String, LoopbackConfiguration> loopbackConfigurations = Maps.newHashMap();	
				for (HierarchicalConfiguration loopbackConfig : loopbackConfigs) {
					LoopbackConfiguration loopbackConfiguration = loadLoopbackConfiguration((String)loopbackConfig.getRoot().getValue());
					if (loopbackConfiguration != null) {
						loopbackConfigurations.put(loopbackConfiguration.getName(), loopbackConfiguration);				
					}
				}
				return loopbackConfigurations;
			}
		} catch (ConfigurationException e) {
			log.error("Unable to load general loopback configurations", e);
		}
		return null;
	}

	private LoopbackConfiguration loadLoopbackConfiguration(String loopbackConfigPath) {
		XMLConfiguration config = new XMLConfiguration();
		try {
			config.load(Loopback.class.getClassLoader().getResourceAsStream(loopbackConfigPath));
			SubnodeConfiguration subConfig = config.configurationAt(LOOPBACK_KEY);
			if (subConfig != null) {
				LoopbackConfiguration loopbackConfig = new LoopbackConfiguration();
				loopbackConfig.setName(subConfig.getString(LOOPBACK_NAME_ATTR));
				loopbackConfig.setPort(subConfig.getInt(LOOPBACK_PORT_ATTR));
				return loopbackConfig;
			}
		} catch (ConfigurationException e) {
			log.error("Unable to load loopback configuration[" + loopbackConfigPath + "]", e);
		}
		return null;
	}
}
