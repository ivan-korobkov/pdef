package io.pdef.descriptors;

import java.util.Set;

public interface SetDescriptor<T> extends ValueDescriptor<Set<T>> {
	/**
	 * Returns a set element descriptor.
	 */
	ValueDescriptor<T> getElement();
}
