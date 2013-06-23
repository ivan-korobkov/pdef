package io.pdef;

import java.util.Map;

public interface InterfaceDescriptor {
	/** Returns a map of this interface methods. */
	Map<String, MethodDescriptor> getMethods();

	/** Creates a new client. */
	Object client(Invocation parent, InvocationHandler handler);

	/** Creates a new server. */
	InvocationHandler server(Object delegate);
}
