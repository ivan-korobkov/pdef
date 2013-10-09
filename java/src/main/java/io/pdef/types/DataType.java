package io.pdef.types;

import io.pdef.internal.InternalJson;

public abstract class DataType<T> extends Type<T> {
	protected DataType(final TypeEnum type) {
		super(type);
	}

	public abstract T copy(final T object);

	// Native format.

	/** Parse an object from a native Java primitive or collection. */
	public final T parseNative(final Object object) throws TypeFormatException {
		try {
			return doParseNative(object);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a native Java primitive or collection. */
	public final Object toNative(final T object) throws TypeFormatException {
		try {
			return doToNative(object);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}
	// String format.

	/** Parse an object from a string. */
	public final T parseString(final String s) throws TypeFormatException {
		try {
			return doParseString(s);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a string. */
	public final String toString(final T object) throws TypeFormatException {
		try {
			return doToString(object);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	// Json format.

	/** Parse an object from a JSON string. */
	public final T parseJson(final String s) throws TypeFormatException {
		try {
			return doParseJson(s);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a JSON string. */
	public final String toJson(final T object) throws TypeFormatException {
		try {
			return doToJson(object, false);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a JSON string. */
	public final String toJson(final T object, final boolean indent)
			throws TypeFormatException {
		try {
			return doToJson(object, indent);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	// Real parsing/serialization.

	/** Parse an object from a native Java primitive or collection. */
	protected abstract T doParseNative(final Object object) throws Exception;

	/** Serialize an object to a native Java primitive or collection. */
	protected abstract Object doToNative(final T object) throws Exception;


	/** Parse an object from a string. */
	protected T doParseString(final String s) throws Exception {
		return doParseJson(s);
	}

	/** Serialize an object to a string. */
	protected String doToString(final T object) throws Exception {
		return doToJson(object, false);
	}

	/** Parse an object from a JSON string. */
	protected T doParseJson(final String s) throws Exception {
		if (s == null) {
			return null;
		}

		Object n = InternalJson.parse(s);
		return doParseNative(n);
	}

	/** Serialize an object to a JSON string. */
	protected String doToJson(final T object, final boolean indent) throws Exception {
		if (object == null) {
			return "null";
		}

		Object n = doToNative(object);
		return InternalJson.serialize(n, indent);
	}
}
