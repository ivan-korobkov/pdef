package io.pdef.meta;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;

import java.util.List;

public class ListType<T> extends DataType<List<T>> {
	private final DataType<T> element;

	public ListType(final DataType<T> element) {
		super(TypeEnum.LIST);
		this.element = checkNotNull(element);
	}

	public DataType<T> getElement() {
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
