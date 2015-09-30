package com.appdirect.loopback.config;

import java.util.Collection;
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

import com.appdirect.loopback.Loopback;
import com.appdirect.loopback.config.model.DelayConfiguration;
import com.appdirect.loopback.config.model.LoopbackConfiguration;
import com.appdirect.loopback.config.model.OAuthConfiguration;
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
	private static final String LOOPBACK_SECUREPORT_ATTR = "[@securePort]";
	private static final String LOOPBACK_RESPONSE_DELAY = "responseDelay";
	private static final String TEMPLATEPATH_KEY = "templatePath";
	private static final String SELECTOR_KEY = "selector";
	private static final String MATCHER_KEY = "matcher";
	private static final String EXTRACTOR_KEY = "extractor";
	private static final String TEMPATE_KEY = "template";
	private static final String SCOPE_ATTR = "[@scope]";

	private static final String OAUTH1 = "oauth1";
	private static final String OAUTH1_CONSUMER_ID = "[@consumerId]";
	private static final String OAUTH1_CONSUMER_SECRET = "[@consumerSecret]";

	private static final String REQUEST_CALLBACK = "requestCallback";
	private static final String REQUEST_CALLBACK_HOST = "[@host]";
	private static final String REQUEST_CALLBACK_TEMPLATE = "[@template]";
	private static final String REQUEST_CALLBACK_DELAY = "[@delay]";

	public Collection<LoopbackConfiguration> loadConfiguration() throws ConfigurationException {
		try {
			Map<String, LoopbackConfiguration> loopbackConfigurations = Maps.newHashMap();
			XMLConfiguration config = new XMLConfiguration();
			config.load(Loopback.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE));
			List<HierarchicalConfiguration> loopbackConfigs = config.configurationsAt(LOOPBACKCONFIG_KEY);
			if (loopbackConfigs != null && !loopbackConfigs.isEmpty()) {
				log.info("Loading configuration from: " + config.getString(LOOPBACKCONFIG_KEY));
				for (HierarchicalConfiguration loopbackConfig : loopbackConfigs) {
					LoopbackConfiguration loopbackConfiguration = loadLoopbackConfiguration((String) loopbackConfig.getRoot().getValue());
					if (loopbackConfigurations.putIfAbsent(loopbackConfiguration.getName(), loopbackConfiguration) != null) {
						throw new ConfigurationException("More than 1 configuration found for " + loopbackConfiguration.getName());
					}
				}
			}
			return loopbackConfigurations.values();
		} catch (ConfigurationException e) {
			throw new ConfigurationException("Unable to load general loopback configurations", e);
		}
	}

	private LoopbackConfiguration loadLoopbackConfiguration(String loopbackConfigPath) throws ConfigurationException {
		XMLConfiguration configuration = new XMLConfiguration();
		try {
			configuration.load(Loopback.class.getClassLoader().getResourceAsStream(loopbackConfigPath));
			SubnodeConfiguration loopbackConfig = configuration.configurationAt(LOOPBACK_KEY);

			if (loopbackConfig == null) {
				throw new ConfigurationException("No configuration define for key [" + LOOPBACK_KEY + "]");
			}

			List<HierarchicalConfiguration> selectorConfigs = loopbackConfig.configurationsAt(SELECTOR_KEY);
			if (selectorConfigs == null || selectorConfigs.isEmpty()) {
				throw new ConfigurationException("No selector define for loopback [" + loopbackConfigPath + "]");
			}

			List<RequestSelector> selectors = Lists.newArrayList();
			for (HierarchicalConfiguration selectorConfig : selectorConfigs) {
				RequestMatcher matcher = loadRequestMatcher(selectorConfig);
				if (matcher == null) {
					throw new ConfigurationException("No matcher define for loopback [" + loopbackConfigPath + "]");
				}

				String selectorName = selectorConfig.getString(LOOPBACK_NAME_ATTR, "NO_NAME");
				log.info("Reading configuration for selector " + selectorName);
				selectors.add(RequestSelector.builder()
						.name(selectorConfig.getString(LOOPBACK_NAME_ATTR))
						.requestMatcher(matcher)
						.requestExtractor(loadRequestExtractor(selectorConfig))
						.template(selectorConfig.getString(TEMPATE_KEY))
						.requestCallback(loadRequestCallbackConfiguration(selectorConfig))
						.oAuthConfiguration(readOauthConfiguration(selectorConfig))
						.build());
			}

			return LoopbackConfiguration.builder()
					.securePort(loopbackConfig.getInt(LOOPBACK_SECUREPORT_ATTR, 0) == 0 ? Optional.<Integer>empty() : Optional.of(loopbackConfig.getInt(LOOPBACK_SECUREPORT_ATTR)))
					.name(loopbackConfig.getString(LOOPBACK_NAME_ATTR))
					.port(loopbackConfig.getInt(LOOPBACK_PORT_ATTR))
					.templatePath(loopbackConfig.getString(TEMPLATEPATH_KEY))
					.selectors(selectors)
					.delayConfiguration(readDelayConfiguration(loopbackConfig))
					.oAuthConfiguration(readOauthConfiguration(loopbackConfig))
					.build();

		} catch (IllegalArgumentException | ConfigurationException e) {
			throw new ConfigurationException("Unable to load loopback configuration[" + loopbackConfigPath + "]", e);
		}
	}

	private Optional<OAuthConfiguration> readOauthConfiguration(HierarchicalConfiguration loopbackConfiguration) throws ConfigurationException {
		try {
			SubnodeConfiguration configNode = loopbackConfiguration.configurationAt(OAUTH1);
			if (configNode == null) {
				return Optional.empty();
			}
			OAuthConfiguration oauthConfig = new OAuthConfiguration(configNode.getString(OAUTH1_CONSUMER_ID), configNode.getString(OAUTH1_CONSUMER_SECRET));
			if (StringUtils.isEmpty(oauthConfig.getConsumerId()) || StringUtils.isEmpty(oauthConfig.getSecret())) {
				throw new ConfigurationException("Invalid OAuth configuration");
			}
			return Optional.of(oauthConfig);
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private DelayConfiguration readDelayConfiguration(SubnodeConfiguration loopbackConfiguration) throws ConfigurationException {
		try {
			SubnodeConfiguration delayConfig = loopbackConfiguration.configurationAt(LOOPBACK_RESPONSE_DELAY);
			DelayConfiguration delayConfiguration = new DelayConfiguration(delayConfig.getInt("min", 0), delayConfig.getInt("max", 0));
			if (delayConfiguration.getMaxDelayMs() < delayConfiguration.getMinDelayMs()) {
				throw new ConfigurationException("Maximum delay MUST be greater than minimum delay.");
			}
			return delayConfiguration;
		} catch (IllegalArgumentException e) {
			return new DelayConfiguration(0, 0);
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

	private RequestMatcher loadRequestMatcher(HierarchicalConfiguration selectorConfig) throws ConfigurationException {
		try {
			SubnodeConfiguration matcherConfig = selectorConfig.configurationAt(MATCHER_KEY);
			RequestMatcher matcher = new RequestMatcher();
			matcher.setScope(Scope.valueOf(matcherConfig.getString(SCOPE_ATTR)));
			matcher.setPattern(Pattern.compile((String) matcherConfig.getRoot().getValue()));
			return matcher;
		} catch (IllegalArgumentException e) {
			throw new ConfigurationException("No request matcher configured.");
		}
	}

	private Optional<RequestCallback> loadRequestCallbackConfiguration(HierarchicalConfiguration selectorConfig) throws ConfigurationException {
		try {
			SubnodeConfiguration requestCallbackConfig = selectorConfig.configurationAt(REQUEST_CALLBACK);
			if (requestCallbackConfig == null) {
				return Optional.empty();
			}

			Optional<RequestCallback> config = Optional.of(new RequestCallback(
					requestCallbackConfig.getString(REQUEST_CALLBACK_TEMPLATE),
					requestCallbackConfig.getString(REQUEST_CALLBACK_HOST),
					requestCallbackConfig.getInt(REQUEST_CALLBACK_DELAY, 10)
			));

			if (StringUtils.isEmpty(config.get().getHost())) {
				throw new ConfigurationException("RequestCallback MUST configure host.");
			}

			if (StringUtils.isEmpty(config.get().getTemplate())) {
				throw new ConfigurationException("RequestCallback MUST template.");
			}

			return config;
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
