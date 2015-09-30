package com.appdirect.loopback;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

public class LoopbackUtils {
	public static final String CRLF = "\r\n";
	public static final String HEADER_DELIMITER = ":";
	public static final String HOST = "Host";

	public static String getMergedTemplate(String templatePath, VelocityContext context) {
		StringWriter stringWriter = new StringWriter();
		Template template = VelocityEngineAccessor.getInstance().getVelocityEngine().getTemplate(templatePath, StandardCharsets.UTF_8.name());
		template.merge(context, stringWriter);
		return stringWriter.toString();
	}
}
