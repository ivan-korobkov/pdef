package io.pdef.descriptors;

import io.pdef.TypeEnum;

public class ValueDescriptor<T> extends Descriptor<T> {
	protected ValueDescriptor(final TypeEnum type, final Class<T> javaClass) {
		super(type, javaClass);

		if (!type.isValueType()) {
			throw new IllegalArgumentException("Type must be a value type, not " + type);
		}
	}
}
