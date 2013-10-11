package io.pdef.descriptors;

/**
 * DataDescriptor is base a descriptor for data classes. These include primitives, collections,
 * enums an messages.
 * */
public abstract class DataDescriptor<T> extends Descriptor {
	protected DataDescriptor(final TypeEnum type) {
		super(type);
	}

	/** Returns a deep copy of an object. */
	public abstract T copy(final T object);
}
