package io.pdef.descriptors;

import java.util.List;

public interface ListDescriptor<T> extends ValueDescriptor<List<T>> {
	/**
	 * Returns a list element descriptor.
	 */
	ValueDescriptor<T> getElement();
}
