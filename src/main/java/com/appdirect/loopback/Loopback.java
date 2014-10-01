package com.appdirect.loopback;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import lombok.extern.java.Log;

import org.eclipse.jetty.server.Server;

@Log
public class Loopback {
	public static void main(String[] args) throws Exception {

		// TODO: do it right
		Properties loopbackDefinitions = new Properties();
		loopbackDefinitions.put("MOSI", "folder location");
		loopbackDefinitions.put("COMCAST", "folder location");

		int port = 8002;

		for (Object loopbackName : loopbackDefinitions.keySet()) {
			log.log(Level.ALL, "Initializing loopback " + loopbackName);

			// TODO: Do it right
			// Properties loopbackProperties =
			// readLoopbackProperties("./loopback/" + config.getValue());
			Properties loopbackProperties = new Properties();
			loopbackProperties.put("port", Integer.toString(port));
			port++;

			LoopbackHandler handler = new LoopbackHandler((String) loopbackName, loopbackProperties);
			Server server = new Server(Integer.parseInt((String) loopbackProperties.get("port")));
			server.setHandler(handler);
			server.start();
		}

		// Probably a better way
		while (true)
			;
	}

	private static Properties readLoopbackProperties(String propertiesPath) throws IOException {
		Properties properties = new Properties();
		InputStream inputStream = Loopback.class.getClassLoader().getResourceAsStream(propertiesPath);
		if (inputStream == null) {
			throw new IOException("properties file not found: " + propertiesPath);
		}
		properties.load(inputStream);
		return properties;
	}
}
