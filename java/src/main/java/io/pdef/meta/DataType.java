package io.pdef.meta;

/**
 * DataType is base a metatype for data classes. These include primitives, collections,
 * enums an messages.
 * */
public abstract class DataType<T> extends MetaType {
	protected DataType(final TypeEnum type) {
		super(type);
	}

	/** Returns a deep copy of an object. */
	public abstract T copy(final T object);
}
