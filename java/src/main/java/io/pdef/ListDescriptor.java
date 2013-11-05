package io.pdef;

import java.util.List;

public interface ListDescriptor<T> extends DataDescriptor<List<T>> {
	/**
	 * Returns a list element descriptor.
	 */
	DataDescriptor<T> getElement();
}
