package io.pdef;

/**
 * DataDescriptor is base a descriptor for data classes. These include primitives, collections,
 * enums an messages.
 */
public abstract class AbstractDataDescriptor<T> extends AbstractDescriptor<T>
		implements DataDescriptor<T> {
	protected AbstractDataDescriptor(final TypeEnum type, final Class<T> javaClass) {
		super(type, javaClass);
	}
}
