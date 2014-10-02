package com.appdirect.loopback;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;

import com.appdirect.loopback.config.LoopbackConfiguration;
import com.appdirect.loopback.config.RequestExtractor;
import com.appdirect.loopback.config.RequestMatcher;
import com.appdirect.loopback.config.RequestSelector;
import com.appdirect.loopback.config.Scope;
import com.google.common.collect.Maps;

@Slf4j
public class Loopback {
	private static final String CONFIGURATION_FILE = "configuration.xml";
	private static final String LOOPBACKCONFIG_KEY = "loopbackConfig";
	private static final String LOOPBACK_KEY = "loopback";
	private static final String LOOPBACK_NAME_ATTR = "[@name]";
	private static final String LOOPBACK_PORT_ATTR = "[@port]";
	private static final String TEMPLATEPATH_KEY = "templatePath";
	private static final String SELECTOR_KEY = "selector";
	private static final String MATCHER_KEY = "matcher";
	private static final String EXTRACTOR_KEY = "extractor";
	private static final String TEMPATE_KEY = "template";
	private static final String SCOPE_ATTR = "[@scope]";

	private final Map<String, Server> servers = Maps.newHashMap();

	public static void main(String[] args) throws Exception {
		Loopback loopback = new Loopback();
		loopback.init();
		loopback.start();
	}

	public void init() throws Exception {
		Map<String, LoopbackConfiguration> loopbackConfig = loadConfiguration();
		if (loopbackConfig != null && !loopbackConfig.isEmpty()) {
			for (Map.Entry<String, LoopbackConfiguration> loopback : loopbackConfig.entrySet()) {
				log.info("Initializing loopback {}", loopback.getKey());
				LoopbackHandler handler = new LoopbackHandler(loopback.getValue());
				Server server = createServer(handler, loopback.getValue().getPort());
				servers.put(loopback.getKey(), server);
			}
		} else {
			log.error("No loopback configuration, cannot continue!");
		}
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
			return;
		}

		for (Map.Entry<String, Server> server : servers.entrySet()) {
			log.info("Starting [" + server.getKey() + "] server...");
			server.getValue().start();
			log.info("[ {} ] started!", server.getKey());
		}
		// Probably a better way
		while (true);
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
				loopbackConfig.setTemplatePath(subConfig.getString(TEMPLATEPATH_KEY));
				List<HierarchicalConfiguration> selectorConfigs = subConfig.configurationsAt(SELECTOR_KEY);
				if (selectorConfigs == null || selectorConfigs.isEmpty()) {
					log.error("No selector define for loopback [" + loopbackConfigPath + "]");
					return null;
				}
				for (HierarchicalConfiguration selectorConfig : selectorConfigs) {
					RequestSelector selector = new RequestSelector();
					RequestMatcher matcher = loadRequestMatcher(selectorConfig);
					if (matcher == null) {
						log.error("No matcher define for loopback [" + loopbackConfigPath + "]");
						continue;
					}
					selector.setRequestMatcher(matcher);
					selector.setRequestExtractor(loadRequestExtractor(selectorConfig));
					selector.setTemplate(selectorConfig.getString(TEMPATE_KEY));
					loopbackConfig.addSelector(selector);
				}
				return loopbackConfig;
			}
		} catch (ConfigurationException e) {
			log.error("Unable to load loopback configuration[" + loopbackConfigPath + "]", e);
		}
		return null;
	}

	private RequestExtractor loadRequestExtractor(HierarchicalConfiguration selectorConfig) {
		try {
			SubnodeConfiguration extractorConfig = selectorConfig.configurationAt(EXTRACTOR_KEY);
			if (extractorConfig != null && !extractorConfig.isEmpty()) {
				RequestExtractor extractor = new RequestExtractor();
				extractor.setScope(Scope.valueOf(extractorConfig.getString(SCOPE_ATTR)));
				extractor.setExtractor(Pattern.compile((String) extractorConfig.getRoot().getValue()));
				return extractor;
			}
		} catch (IllegalArgumentException e) {
			// No extractor...
		}
		return null;
	}

	private RequestMatcher loadRequestMatcher(HierarchicalConfiguration selectorConfig) {
		SubnodeConfiguration matcherConfig = selectorConfig.configurationAt(MATCHER_KEY);
		if (matcherConfig != null && !matcherConfig.isEmpty()) {
			RequestMatcher matcher = new RequestMatcher();
			matcher.setScope(Scope.valueOf(matcherConfig.getString(SCOPE_ATTR)));
			matcher.setPattern(Pattern.compile((String) matcherConfig.getRoot().getValue()));
			return matcher;
		}
		return null;
	}
}
