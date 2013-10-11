package io.pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;

import java.util.List;

public class ListDescriptor<T> extends DataDescriptor<List<T>> {
	private final DataDescriptor<T> element;

	public ListDescriptor(final DataDescriptor<T> element) {
		super(TypeEnum.LIST);
		this.element = checkNotNull(element);
	}

	public DataDescriptor<T> getElement() {
		return element;
	}

	@Override
	public List<T> copy(final List<T> list) {
		if (list == null) {
			return null;
		}

		List<T> copy = Lists.newArrayList();
		for (T e : list) {
			T copied = element.copy(e);
			copy.add(copied);
		}

		return copy;
	}
}
