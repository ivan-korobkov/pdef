package io.pdef.descriptors;

public interface FieldDescriptor<M, V> extends FieldAccessor<M, V> {
	/**
	 * Returns a pdef field name.
	 */
	String getName();

	/**
	 * Returns a field type descriptor.
	 */
	ValueDescriptor<V> getType();

	/**
	 * Returns whether this field is a discriminator in a message.
	 */
	boolean isDiscriminator();
}
