package io.pdef;

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
	INTERFACE
}
