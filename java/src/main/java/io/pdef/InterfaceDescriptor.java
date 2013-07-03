package io.pdef;

import java.lang.reflect.InvocationHandler;
import java.util.Map;

public interface InterfaceDescriptor<T> {
	/** Returns a java class of this descriptor. */
	Class<T> getJavaClass();

	/** Returns a map of this interface methods. */
	Map<String, MethodDescriptor> getMethods();

	/** Returns a method by its name or null. */
	MethodDescriptor getMethod(String method);

	/** Creates a new proxy. */
	T proxy(InvocationHandler handler);
}
