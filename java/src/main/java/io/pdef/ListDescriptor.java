package io.pdef;

import java.util.List;

public interface ListDescriptor<T> extends DataTypeDescriptor<List<T>> {
	/**
	 * Returns a list element descriptor.
	 */
	DataTypeDescriptor<T> getElement();
}
