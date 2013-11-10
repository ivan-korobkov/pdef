package io.pdef;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** TypeEnum enumerates Pdef types. */
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

	/** Returns a pdef value type of a java class or throws IllegalArgumentException. */
	public static TypeEnum valueTypeOf(final Class<?> cls) {
		if (cls == null) throw new NullPointerException("cls");
		else if (cls == Boolean.class) return TypeEnum.BOOL;
		else if (cls == Short.class) return TypeEnum.INT16;
		else if (cls == Integer.class) return TypeEnum.INT32;
		else if (cls == Long.class) return TypeEnum.INT64;
		else if (cls == Float.class) return TypeEnum.FLOAT;
		else if (cls == Double.class) return TypeEnum.DOUBLE;
		else if (cls == String.class) return TypeEnum.STRING;
		else if (List.class.isAssignableFrom(cls)) return TypeEnum.LIST;
		else if (Set.class.isAssignableFrom(cls)) return TypeEnum.SET;
		else if (Map.class.isAssignableFrom(cls)) return TypeEnum.MAP;
		else if (cls == Void.class) return TypeEnum.VOID;
		else if (cls.isEnum()) return TypeEnum.ENUM;
		else if (Exception.class.isAssignableFrom(cls) && Message.class.isAssignableFrom(cls)) {
			return TypeEnum.EXCEPTION;
		} else if (Message.class.isAssignableFrom(cls)) return TypeEnum.MESSAGE;
		throw new IllegalArgumentException("Unsupported value type " + cls);
	}

	public boolean isValueType() {
		return this != INTERFACE;
	}
}
