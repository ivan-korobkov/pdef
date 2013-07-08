package io.pdef;

public interface PrimitiveDescriptor<T> extends Descriptor<T> {

	T parseFromString(String s);

	String serializeToString(T object);
}
