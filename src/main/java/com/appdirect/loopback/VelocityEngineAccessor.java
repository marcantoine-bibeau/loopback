package com.appdirect.loopback;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.IncludeRelativePath;
import org.apache.velocity.runtime.RuntimeConstants;

public class VelocityEngineAccessor {
	private static final VelocityEngineAccessor instance = new VelocityEngineAccessor();
	private final VelocityEngine velocityEngine = new VelocityEngine();

	private VelocityEngineAccessor() {
		this.velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		//this.velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
		this.velocityEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		this.velocityEngine.setProperty(RuntimeConstants.EVENTHANDLER_INCLUDE, IncludeRelativePath.class.getName());
		this.velocityEngine.init();
	}

	public static VelocityEngineAccessor getInstance() {
		return instance;
	}

	public VelocityEngine getVelocityEngine() {
		return velocityEngine;
	}
}
