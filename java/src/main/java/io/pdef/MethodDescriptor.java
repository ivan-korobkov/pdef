package io.pdef;

import io.pdef.rpc.MethodCall;

import java.util.Map;

public interface MethodDescriptor {
	String getName();

	Map<String, Descriptor<?>> getArgs();

	boolean isRemote();

	Descriptor<?> getResult();

	Descriptor<?> getExc();

	InterfaceDescriptor<?> getNext();

	Invocation capture(Invocation parent, Object... args);

	Invocation parse(Invocation parent, Map<String, Object> args);

	Object invoke(Object object, Map<String, Object> args);

	MethodCall serialize(Map<String, Object> args);
}
