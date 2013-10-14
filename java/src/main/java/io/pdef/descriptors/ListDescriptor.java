package io.pdef.descriptors;

import java.util.ArrayList;
import java.util.List;

public class ListDescriptor<T> extends DataDescriptor<List<T>> {
	private final DataDescriptor<T> element;

	@SuppressWarnings("unchecked")
	ListDescriptor(final DataDescriptor<T> element) {
		super(TypeEnum.LIST, (Class<List<T>>) (Class<?>) List.class);
		this.element = element;
		if (element == null) throw new NullPointerException("element");
	}

	public DataDescriptor<T> getElement() {
		return element;
	}

	@Override
	public List<T> copy(final List<T> list) {
		if (list == null) {
			return null;
		}

		List<T> copy = new ArrayList<T>();
		for (T e : list) {
			T copied = element.copy(e);
			copy.add(copied);
		}

		return copy;
	}
}
