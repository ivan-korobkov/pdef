package io.pdef.invocation;

import java.util.List;

public interface InvocationParser {

	List<Invocation> parse(Class<?> interfaceClass, Object object);
}
