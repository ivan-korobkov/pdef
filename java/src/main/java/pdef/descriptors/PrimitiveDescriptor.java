package pdef.descriptors;

public interface PrimitiveDescriptor<T> extends Descriptor<T> {

	/** Parses a primitive from a string. */
	T parseFromString(String s);

	/** Serializes a primitive to a string. */
	String serializeToString(T object);
}
