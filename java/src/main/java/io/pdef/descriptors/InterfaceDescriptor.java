package io.pdef.descriptors;

import javax.annotation.Nullable;
import java.util.List;

public interface InterfaceDescriptor<T> extends Descriptor<T> {
	/**
	 * Returns a method descriptor by its name and returns it or {@literal null}.
	 */
	@Nullable
	MethodDescriptor<T, ?> getMethod(String name);

	/**
	 * Returns a list of method descriptors or an empty list.
	 */
	List<MethodDescriptor<T, ?>> getMethods();

	/**
	 * Returns an exception descriptor or {@literal null}.
	 */
	@Nullable
	MessageDescriptor<?> getExc();

	/**
	 * Returns an index method descriptor or {@literal null}.
	 */
	@Nullable
	MethodDescriptor<T, ?> getIndexMethod();
}
