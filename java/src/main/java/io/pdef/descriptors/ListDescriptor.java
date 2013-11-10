package io.pdef.descriptors;

import io.pdef.TypeEnum;

import java.util.List;

public class ListDescriptor<T> extends ValueDescriptor<List<T>> {
	private final ValueDescriptor<T> element;

	@SuppressWarnings("unchecked")
	ListDescriptor(final ValueDescriptor<T> element) {
		super(TypeEnum.LIST, (Class<List<T>>) (Class<?>) List.class);
		if (element == null) throw new NullPointerException("element");

		this.element = element;
	}

	/** Returns a list element descriptor. */
	public ValueDescriptor<T> getElement() {
		return element;
	}
}
