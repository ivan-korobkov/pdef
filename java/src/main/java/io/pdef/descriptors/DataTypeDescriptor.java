package io.pdef.descriptors;

import io.pdef.TypeEnum;

public class DataTypeDescriptor<T> extends Descriptor<T> {
	protected DataTypeDescriptor(final TypeEnum type, final Class<T> javaClass) {
		super(type, javaClass);

		if (!type.isDataType()) {
			throw new IllegalArgumentException("Type must be a data type, not " + type);
		}
	}
}
