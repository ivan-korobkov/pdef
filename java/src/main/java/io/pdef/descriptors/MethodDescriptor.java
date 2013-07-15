package io.pdef.descriptors;

import io.pdef.Invocation;
import io.pdef.rpc.RpcCall;

import javax.annotation.Nullable;
import java.util.Map;

/** Interface method descriptor. */
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

	/** Creates a new proxy for this method. */
	Invocation capture(Invocation parent, Object... args);

	/** Invokes this method on an object with a given args. */
	Object invoke(Object object, Object... args);

	/** Serializes arguments into a method call. */
	RpcCall serialize(Object... args);

	/** Parses arguments into an proxy. */
	Invocation parse(Invocation parent, Map<String, Object> args);
}
