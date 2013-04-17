package io.pdef.invocation;

import java.util.List;

public interface InvocationParser {

	List<Invocation> parseInvocations(Class<?> interfaceClass, Object object);
}
