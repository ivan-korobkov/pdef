package io.pdef.types;

import io.pdef.internal.InternalJson;

public abstract class DataType extends Type {
	protected DataType(final TypeEnum type, final Class<?> javaClass) {
		super(type, javaClass);
	}

	public abstract Object copy(final Object object);

	// Native format.

	/** Parse an object from a native Java primitive or collection. */
	public final Object parseNative(final Object o) throws TypeFormatException {
		try {
			return doParseNative(o);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a native Java primitive or collection. */
	public final Object toNative(final Object o) throws TypeFormatException {
		try {
			return doToNative(o);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}
	// String format.

	/** Parse an object from a string. */
	public final Object parseString(final String s) throws TypeFormatException {
		try {
			return doParseString(s);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a string. */
	public final String toString(final Object o) throws TypeFormatException {
		try {
			return doToString(o);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	// Json format.

	/** Parse an object from a JSON string. */
	public final Object parseJson(final String s) throws TypeFormatException {
		try {
			return doParseJson(s);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a JSON string. */
	public final String toJson(final Object o) throws TypeFormatException {
		try {
			return doToJson(o, false);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a JSON string. */
	public final String toJson(final Object o, final boolean indent)
			throws TypeFormatException {
		try {
			return doToJson(o, indent);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	// Real parsing/serialization.

	/** Parse an object from a native Java primitive or collection. */
	protected abstract Object doParseNative(final Object o) throws Exception;

	/** Serialize an object to a native Java primitive or collection. */
	protected abstract Object doToNative(final Object o) throws Exception;


	/** Parse an object from a string. */
	protected Object doParseString(final String s) throws Exception {
		return doParseJson(s);
	}

	/** Serialize an object to a string. */
	protected String doToString(final Object o) throws Exception {
		return doToJson(o, false);
	}

	/** Parse an object from a JSON string. */
	public Object doParseJson(final String s) throws Exception {
		if (s == null) {
			return null;
		}

		Object n = InternalJson.parse(s);
		return doParseNative(n);
	}

	/** Serialize an object to a JSON string. */
	public String doToJson(final Object o, final boolean indent) throws Exception {
		if (o == null) {
			return "null";
		}

		Object n = doToNative(o);
		return InternalJson.serialize(n, indent);
	}
}
