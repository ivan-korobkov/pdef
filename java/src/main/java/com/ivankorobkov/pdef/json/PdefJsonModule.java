package com.ivankorobkov.pdef.json;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class PdefJsonModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(JsonFormat.class).to(JsonFormatImpl.class).in(Singleton.class);
		bind(LineFormat.class).to(LineFormatImpl.class).in(Singleton.class);
	}
}
