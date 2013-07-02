package io.pdef;

import io.pdef.rpc.MethodCall;

import javax.annotation.Nullable;
import java.util.Map;

public interface MethodDescriptor {
	/** Returns this method name. */
	String getName();

	/** Returns a map of method arg descriptors. */
	Map<String, Descriptor<?>> getArgs();

	/** Returns true when a method result is a data type. */
	boolean isRemote();

	/** Returns a method result descriptor when remote or null. */
	@Nullable
	Descriptor<?> getResult();

	/** Returns an exception descriptor. */
	@Nullable
	Descriptor<?> getExc();

	/** Returns an interface descriptor (method result) when is not remote or null. */
	@Nullable
	InterfaceDescriptor<?> getNext();

	/** Creates a new invocation for this method. */
	Invocation capture(Invocation parent, Object... args);

	/** Invokes this method on an object with a given args. */
	Object invoke(Object object, Object... args);

	/** Serializes arguments into a method call. */
	MethodCall serialize(Object... args);

	/** Parses arguments into an invocation. */
	Invocation parse(Invocation parent, Map<String, Object> args);
}
