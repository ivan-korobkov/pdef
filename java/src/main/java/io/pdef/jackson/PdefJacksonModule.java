package io.pdef.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

public class PdefJacksonModule extends Module {

	@Override
	public String getModuleName() {
		return "pdef-jackson";
	}

	@Override
	public Version version() {
		return Version.unknownVersion();
	}

	@Override
	public void setupModule(final SetupContext context) {
		context.appendAnnotationIntrospector(new PdefJacksonAnnotationIntrospector());
	}
}
