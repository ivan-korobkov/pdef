package io.pdef;

import java.util.Map;

public interface MethodDescriptor {
	String getName();

	Invocation capture(Invocation parent, Object... args);

	Invocation parse(Invocation invocation, Map<String, Object> args);
}
