package io.pdef;

import io.pdef.rpc.MethodCall;

import java.util.Map;

public interface MethodDescriptor {
	String getName();

	boolean isRemote();

	Map<String, Descriptor<?>> getArgs();

	Descriptor<?> getResult();

	Descriptor<?> getResultExc();

	InterfaceDescriptor getResultInterface();

	Invocation capture(Invocation parent, Object... args);

	Object invoke(Object delegate, Invocation invocation);

	MethodCall write(Invocation invocation);

	Invocation read(MethodCall call);
}
