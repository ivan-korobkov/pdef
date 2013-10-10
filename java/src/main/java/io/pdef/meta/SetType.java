package io.pdef.meta;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Sets;

import java.util.Set;

public class SetType<T> extends DataType<Set<T>> {
	private final DataType<T> element;

	public SetType(final DataType<T> element) {
		super(TypeEnum.SET);
		this.element = checkNotNull(element);
	}

	public DataType<T> getElement() {
		return element;
	}

	@Override
	public Set<T> copy(final Set<T> set) {
		if (set == null) {
			return null;
		}

		Set<T> copy = Sets.newHashSet();
		for (T elem : set) {
			T copied = element.copy(elem);
			copy.add(copied);
		}

		return copy;
	}
}
