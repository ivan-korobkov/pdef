package io.pdef.descriptors;

import java.util.EnumSet;

/**
 * TypeEnum enumerates Pdef types.
 * */
public enum TypeEnum {
	// Primitives.
	BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING,

	// Collections.
	LIST, SET, MAP,

	// Void type (can be used only as a method result).
	VOID,

	// User-defined types.
	ENUM,
	MESSAGE,
	EXCEPTION,
	INTERFACE;

	private static final EnumSet<TypeEnum> PRIMITIVES;
	private static final EnumSet<TypeEnum> DATA_TYPES;

	static {
		PRIMITIVES = EnumSet.of(BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING);
		DATA_TYPES = EnumSet.copyOf(PRIMITIVES);
		DATA_TYPES.add(LIST);
		DATA_TYPES.add(SET);
		DATA_TYPES.add(MAP);
		DATA_TYPES.add(ENUM);
		DATA_TYPES.add(MESSAGE);
		DATA_TYPES.add(EXCEPTION);
	}

	public boolean isPrimitive() {
		return PRIMITIVES.contains(this);
	}

	public boolean isDataType() {
		return DATA_TYPES.contains(this);
	}

	public boolean isMessage() {
		return this == MESSAGE || this == EXCEPTION;
	}

	public boolean isCollection() {
		return this == LIST || this == SET || this == MAP;
	}

	public boolean isEnum() {
		return this == ENUM;
	}
}
