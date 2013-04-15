package io.pdef;

import java.lang.reflect.Type;

public interface Parser {

	Object parse(Type type, Object object);
}
