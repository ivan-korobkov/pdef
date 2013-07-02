package io.pdef;

import java.lang.reflect.InvocationHandler;
import java.util.Map;

public interface InterfaceDescriptor<T> {
	/** Returns a java class of this descriptor. */
	Class<T> getJavaClass();

	/** Returns a map of this interface methods. */
	Map<String, MethodDescriptor> getMethods();

	/** Creates a new proxy. */
	T proxy(InvocationHandler handler);
}
