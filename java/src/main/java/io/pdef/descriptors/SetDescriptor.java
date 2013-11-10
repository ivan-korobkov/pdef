package io.pdef.descriptors;

import io.pdef.TypeEnum;

import java.util.Set;

public class SetDescriptor<T> extends ValueDescriptor<Set<T>> {
	private final ValueDescriptor<T> element;

	@SuppressWarnings("unchecked")
	SetDescriptor(final ValueDescriptor<T> element) {
		super(TypeEnum.SET, (Class<Set<T>>) (Class<?>) Set.class);
		if (element == null) throw new NullPointerException("element");

		this.element = element;
	}

	/** Returns a set element descriptor. */
	public ValueDescriptor<T> getElement() {
		return element;
	}
}
