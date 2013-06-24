package io.pdef;

import java.util.Map;

public interface InterfaceType<T> {
	/** Returns a java class of this descriptor. */
	Class<T> getJavaClass();

	/** Returns a map of this interface methods. */
	Map<String, Method> getMethods();

	/** Creates a new client. */
	T client(InvocationHandler handler);

	/** Creates a new client. */
	T client(InvocationHandler handler, Invocation parent);

	/** Creates a new server. */
	InvocationHandler server(T delegate);
}
