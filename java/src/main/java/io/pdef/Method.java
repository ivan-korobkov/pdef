package io.pdef;

import io.pdef.rpc.MethodCall;

import java.util.Map;

public interface Method {
	String getName();

	boolean isRemote();

	Map<String, Type<?>> getArgs();

	Type<?> getResult();

	Type<?> getResultExc();

	InterfaceType getResultInterface();

	Invocation capture(Invocation parent, Object... args);

	Object invoke(Object delegate, Invocation invocation);

	MethodCall write(Invocation invocation);

	Invocation read(MethodCall call);
}
