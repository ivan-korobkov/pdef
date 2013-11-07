package io.pdef;

import java.util.Set;

public interface SetDescriptor<T> extends DataTypeDescriptor<Set<T>> {
	/**
	 * Returns a set element descriptor.
	 */
	DataTypeDescriptor<T> getElement();
}
