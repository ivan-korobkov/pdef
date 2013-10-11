package io.pdef.descriptors;

public interface FieldSetter<M, V> {
	void set(M message, V value);
}
