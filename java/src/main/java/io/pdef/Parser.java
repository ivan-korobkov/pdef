package io.pdef;

import java.lang.reflect.Type;
import java.util.List;

public interface Parser {

	Object parse(Type type, Object object);

	List<Invocation> parseInvocations(Class<?> interfaceClass, Object object);
}
