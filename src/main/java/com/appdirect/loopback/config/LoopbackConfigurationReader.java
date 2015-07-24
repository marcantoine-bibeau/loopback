package com.appdirect.loopback.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpMethod;

import com.appdirect.loopback.Loopback;
import com.appdirect.loopback.config.model.DelayConfiguration;
import com.appdirect.loopback.config.model.LoopbackConfiguration;
import com.appdirect.loopback.config.model.RequestCallback;
import com.appdirect.loopback.config.model.RequestExtractor;
import com.appdirect.loopback.config.model.RequestMatcher;
import com.appdirect.loopback.config.model.RequestSelector;
import com.appdirect.loopback.config.model.Scope;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
public class LoopbackConfigurationReader {
	private static final String CONFIGURATION_FILE = "configuration.xml";
	private static final String LOOPBACKCONFIG_KEY = "loopbackConfig";
	private static final String LOOPBACK_KEY = "loopback";
	private static final String LOOPBACK_NAME_ATTR = "[@name]";
	private static final String LOOPBACK_PORT_ATTR = "[@port]";
	private static final String LOOPBACK_IS_SSL = "[@ssl]";
	private static final String LOOPBACK_RESPONSE_DELAY = "responseDelay";
	private static final String TEMPLATEPATH_KEY = "templatePath";
	private static final String SELECTOR_KEY = "selector";
	private static final String MATCHER_KEY = "matcher";
	private static final String EXTRACTOR_KEY = "extractor";
	private static final String TEMPATE_KEY = "template";
	private static final String SCOPE_ATTR = "[@scope]";

	private static final String REQUEST_CALLBACK = "requestCallback";
	private static final String REQUEST_CALLBACK_METHOD = "[@method]";
	private static final String REQUEST_CALLBACK_PATH = "[@path]";
	private static final String REQUEST_CALLBACK_HOST = "[@host]";
	private static final String REQUEST_CALLBACK_PORT = "[@port]";

	public Map<String, LoopbackConfiguration> loadConfiguration() throws IllegalConfigurationException {
		try {
			Map<String, LoopbackConfiguration> loopbackConfigurations = Maps.newHashMap();
			XMLConfiguration config = new XMLConfiguration();
			config.load(Loopback.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE));
			List<HierarchicalConfiguration> loopbackConfigs = config.configurationsAt(LOOPBACKCONFIG_KEY);
			if (loopbackConfigs != null && !loopbackConfigs.isEmpty()) {
				for (HierarchicalConfiguration loopbackConfig : loopbackConfigs) {
					LoopbackConfiguration loopbackConfiguration = loadLoopbackConfiguration((String) loopbackConfig.getRoot().getValue());
					loopbackConfigurations.put(loopbackConfiguration.getName(), loopbackConfiguration);
				}
			}
			return loopbackConfigurations;
		} catch (ConfigurationException e) {
			throw new IllegalConfigurationException("Unable to load general loopback configurations", e);
		}
	}

	private LoopbackConfiguration loadLoopbackConfiguration(String loopbackConfigPath) throws IllegalConfigurationException {
		XMLConfiguration configuration = new XMLConfiguration();
		try {
			configuration.load(Loopback.class.getClassLoader().getResourceAsStream(loopbackConfigPath));
			SubnodeConfiguration loopbackConfig = configuration.configurationAt(LOOPBACK_KEY);

			if (loopbackConfig == null) {
				throw new ConfigurationException("No configuration define for key [" + LOOPBACK_KEY + "]");
			}

			List<HierarchicalConfiguration> selectorConfigs = loopbackConfig.configurationsAt(SELECTOR_KEY);
			if (selectorConfigs == null || selectorConfigs.isEmpty()) {
				throw new IllegalConfigurationException("No selector define for loopback [" + loopbackConfigPath + "]");
			}

			List<RequestSelector> selectors = Lists.newArrayList();
			for (HierarchicalConfiguration selectorConfig : selectorConfigs) {
				RequestMatcher matcher = loadRequestMatcher(selectorConfig);
				if (matcher == null) {
					throw new IllegalConfigurationException("No matcher define for loopback [" + loopbackConfigPath + "]");
				}

				selectors.add(RequestSelector.builder()
						.requestMatcher(matcher)
						.requestExtractor(loadRequestExtractor(selectorConfig))
						.template(selectorConfig.getString(TEMPATE_KEY))
						.requestCallback(loadRequestCallback(selectorConfig))
						.build());
			}

			return LoopbackConfiguration.builder()
					.isSSL(false)
					.name(loopbackConfig.getString(LOOPBACK_NAME_ATTR))
					.port(loopbackConfig.getInt(LOOPBACK_PORT_ATTR))
					.templatePath(loopbackConfig.getString(TEMPLATEPATH_KEY))
					.selectors(selectors)
					.delayConfiguration(readDelayConfiguration(loopbackConfig))
					.build();

		} catch (IllegalArgumentException | ConfigurationException e) {
			throw new IllegalConfigurationException("Unable to load loopback configuration[" + loopbackConfigPath + "]", e);
		}
	}

	private Optional<DelayConfiguration> readDelayConfiguration(SubnodeConfiguration loopbackConfiguration) throws ConfigurationException {
		try {
			SubnodeConfiguration delayConfig = loopbackConfiguration.configurationAt(LOOPBACK_RESPONSE_DELAY);
			DelayConfiguration delayConfiguration = new DelayConfiguration(delayConfig.getInt("min", 0), delayConfig.getInt("max", 0));
			if (delayConfiguration.getMaxDelayMs() < delayConfiguration.getMinDelayMs()) {
				throw new ConfigurationException("Maximum delay MUST be greater than minimum delay.");
			}
			return Optional.of(delayConfiguration);
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private Optional<RequestExtractor> loadRequestExtractor(HierarchicalConfiguration selectorConfig) {
		try {
			SubnodeConfiguration extractorConfig = selectorConfig.configurationAt(EXTRACTOR_KEY);
			RequestExtractor extractor = new RequestExtractor();
			extractor.setScope(Scope.valueOf(extractorConfig.getString(SCOPE_ATTR)));
			extractor.setPattern(Pattern.compile((String) extractorConfig.getRoot().getValue()));
			return Optional.of(extractor);
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private RequestMatcher loadRequestMatcher(HierarchicalConfiguration selectorConfig) throws IllegalConfigurationException {
		try {
			SubnodeConfiguration matcherConfig = selectorConfig.configurationAt(MATCHER_KEY);
			RequestMatcher matcher = new RequestMatcher();
			matcher.setScope(Scope.valueOf(matcherConfig.getString(SCOPE_ATTR)));
			matcher.setPattern(Pattern.compile((String) matcherConfig.getRoot().getValue()));
			return matcher;
		} catch (IllegalArgumentException e) {
			throw new IllegalConfigurationException("No request matcher configured.");
		}
	}

	private Optional<RequestCallback> loadRequestCallback(HierarchicalConfiguration selectorConfig) {
		try {
			SubnodeConfiguration requestCallbackConfig = selectorConfig.configurationAt(REQUEST_CALLBACK);
			return Optional.of(RequestCallback.builder()
					.method(StringUtils.isEmpty(requestCallbackConfig.getString(REQUEST_CALLBACK_METHOD)) ? null : HttpMethod.valueOf(requestCallbackConfig.getString(REQUEST_CALLBACK_METHOD)))
					.path(requestCallbackConfig.getString(REQUEST_CALLBACK_PATH))
					.template(requestCallbackConfig.getString(""))
					.host(requestCallbackConfig.getString(REQUEST_CALLBACK_HOST, "localhost"))
					.port(requestCallbackConfig.getInt(REQUEST_CALLBACK_PORT, 80))
					.build());
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
