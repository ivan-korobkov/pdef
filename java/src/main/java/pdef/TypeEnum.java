package pdef;

import java.util.EnumSet;

public enum TypeEnum {
	// Primitives.
	BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING,

	// Collections.
	LIST, SET, MAP,

	// Special types.
	OBJECT, VOID,

	// User-defined types.
	ENUM,
	MESSAGE,
	EXCEPTION,
	INTERFACE;

	private static final EnumSet<TypeEnum> PRIMITIVES = EnumSet.of(
			BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING);

	public boolean isPrimitive() {
		return PRIMITIVES.contains(this);
	}

	public boolean isDataType() {
		return this != INTERFACE;
	}
}
