package io.pdef;

/**
 * DataTypeDescriptor is base a descriptor for data classes. These include primitives, collections,
 * enums an messages.
 */
public abstract class AbstractDataTypeDescriptor<T> extends AbstractDescriptor<T>
		implements DataTypeDescriptor<T> {
	protected AbstractDataTypeDescriptor(final TypeEnum type, final Class<T> javaClass) {
		super(type, javaClass);
	}
}
