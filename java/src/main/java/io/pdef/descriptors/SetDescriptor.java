package io.pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Sets;

import java.util.Set;

public class SetDescriptor<T> extends DataDescriptor<Set<T>> {
	private final DataDescriptor<T> element;

	@SuppressWarnings("unchecked")
	SetDescriptor(final DataDescriptor<T> element) {
		super(TypeEnum.SET, (Class<Set<T>>) (Class<?>) Set.class);
		this.element = checkNotNull(element);
	}

	public DataDescriptor<T> getElement() {
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
