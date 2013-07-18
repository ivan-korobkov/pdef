package io.pdef.descriptors;

import com.google.common.base.Function;
import io.pdef.Invocation;
import io.pdef.InvocationResult;

import java.util.Map;

/** Interface descriptor. */
public interface InterfaceDescriptor<T> {
	/** Returns this descriptor interface java class. */
	Class<T> getJavaClass();

	/** Returns a map of this interface methods. */
	Map<String, MethodDescriptor> getMethods();

	/** Returns a method by its name or null. */
	MethodDescriptor getMethod(String method);

	/** Returns true when a method is present. */
	boolean hasMethod(String name);

	/** Creates a new client. */
	T client(Function<Invocation, InvocationResult> handler);

	/** Creates a new client which continues a given invocation. */
	T client(Invocation parent, Function<Invocation, InvocationResult> handler);

}
