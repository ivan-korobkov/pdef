package io.pdef;

import java.util.Set;

public interface SetDescriptor<T> extends DataDescriptor<Set<T>> {
	/**
	 * Returns a set element descriptor.
	 */
	DataDescriptor<T> getElement();
}
