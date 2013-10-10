package io.pdef.types;

import io.pdef.internal.InternalJson;

public abstract class DataType<T> extends Type<T> {
	protected DataType(final TypeEnum type) {
		super(type);
	}

	public abstract T copy(final T object);

	// Native format.

	/** Parse an object from a native Java primitive or collection. */
	public final T parseFromNative(final Object object) throws TypeFormatException {
		try {
			return fromNative(object);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a native Java primitive or collection. */
	public final Object serializeToNative(final T object) throws TypeFormatException {
		try {
			return toNative(object);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}
	// String format.

	/** Parse an object from a string. */
	public final T parseFromString(final String s) throws TypeFormatException {
		try {
			return fromString(s);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a string. */
	public final String serializeToString(final T object) throws TypeFormatException {
		try {
			return toString(object);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	// Json format.

	/** Parse an object from a JSON string. */
	public final T parseFromJson(final String s) throws TypeFormatException {
		try {
			return fromJson(s);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a JSON string. */
	public final String serializeToJson(final T object) throws TypeFormatException {
		try {
			return toJson(object, false);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	/** Serialize an object to a JSON string. */
	public final String serializeToJson(final T object, final boolean indent)
			throws TypeFormatException {
		try {
			return toJson(object, indent);
		} catch (TypeFormatException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeFormatException(e);
		}
	}

	// Real parsing/serialization.

	/** Parse an object from a native Java primitive or collection. */
	protected abstract T fromNative(final Object object) throws Exception;

	/** Serialize an object to a native Java primitive or collection. */
	protected abstract Object toNative(final T object) throws Exception;


	/** Parse an object from a string. */
	protected T fromString(final String s) throws Exception {
		return fromJson(s);
	}

	/** Serialize an object to a string. */
	protected String toString(final T object) throws Exception {
		return toJson(object, false);
	}

	/** Parse an object from a JSON string. */
	protected T fromJson(final String s) throws Exception {
		if (s == null) {
			return null;
		}

		Object n = InternalJson.parse(s);
		return fromNative(n);
	}

	/** Serialize an object to a JSON string. */
	protected String toJson(final T object, final boolean indent) throws Exception {
		if (object == null) {
			return "null";
		}

		Object n = toNative(object);
		return InternalJson.serialize(n, indent);
	}
}
