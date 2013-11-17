package io.pdef.descriptors;

import io.pdef.TypeEnum;

import java.util.Set;

public class SetDescriptor<T> extends DataTypeDescriptor<Set<T>> {
	private final DataTypeDescriptor<T> element;

	@SuppressWarnings("unchecked")
	SetDescriptor(final DataTypeDescriptor<T> element) {
		super(TypeEnum.SET, (Class<Set<T>>) (Class<?>) Set.class);
		if (element == null) throw new NullPointerException("element");

		this.element = element;
	}

	/** Returns a set element descriptor. */
	public DataTypeDescriptor<T> getElement() {
		return element;
	}
}
