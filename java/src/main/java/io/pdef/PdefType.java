package io.pdef;

import java.util.EnumSet;

/** Pdef types. */
public enum PdefType {
	INTERFACE,

	// Data types
	MESSAGE, OBJECT, LIST, SET, MAP,

	// Primitives
	ENUM, BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, DECIMAL, DATE, DATETIME, STRING, UUID,

	// Special method result types.
	VOID, FUTURE;

	private static final EnumSet<PdefType> PRIMITIVES;
	private static final EnumSet<PdefType> DATATYPES;

	static {
		PRIMITIVES = EnumSet.of(BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, DECIMAL,
				DATE, DATETIME, STRING, UUID, ENUM);

		DATATYPES = EnumSet.of(FUTURE, VOID, MESSAGE, OBJECT, LIST, SET, MAP);
		DATATYPES.addAll(PRIMITIVES);
	}

	public boolean isInterface() {
		return this == INTERFACE;
	}

	public boolean isFuture() {
		return this == FUTURE;
	}

	public boolean isVoid() {
		return this == VOID;
	}

	public boolean isDatatype() {
		return DATATYPES.contains(this);
	}

	public boolean isPrimitive() {
		return PRIMITIVES.contains(this);
	}
}
