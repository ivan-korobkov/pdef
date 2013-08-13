package pdef.descriptors;

/** Data type descriptor. */
public interface Descriptor<T> {
	/** Returns this descriptor data type java class. */
	Class<T> getJavaClass();

	/** Returns the default value for this data type. */
	T getDefault();

	/** Parses a data type from an object. */
	T parse(Object object);

	/** Serializes a data type into an object. */
	Object serialize(T object);
}
