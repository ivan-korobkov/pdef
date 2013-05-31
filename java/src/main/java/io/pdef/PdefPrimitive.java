package io.pdef;

import java.lang.reflect.Type;

public class PdefPrimitive extends PdefDatatype {
	private final Object defaultValue;

	PdefPrimitive(final PdefType type, final Type javaType, final Object defaultValue,
			final Pdef pdef) {
		super(type, javaType, pdef);
		this.defaultValue = defaultValue;
	}

	@Override
	public Object defaultValue() {
		return defaultValue;
	}
}
