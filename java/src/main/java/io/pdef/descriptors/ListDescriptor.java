package io.pdef.descriptors;

import io.pdef.TypeEnum;

import java.util.List;

public class ListDescriptor<T> extends DataTypeDescriptor<List<T>> {
	private final DataTypeDescriptor<T> element;

	@SuppressWarnings("unchecked")
	ListDescriptor(final DataTypeDescriptor<T> element) {
		super(TypeEnum.LIST, (Class<List<T>>) (Class<?>) List.class);
		if (element == null) throw new NullPointerException("element");

		this.element = element;
	}

	/** Returns a list element descriptor. */
	public DataTypeDescriptor<T> getElement() {
		return element;
	}
}
