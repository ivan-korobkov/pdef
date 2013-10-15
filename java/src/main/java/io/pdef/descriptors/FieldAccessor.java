package io.pdef.descriptors;

public interface FieldAccessor<M, V> {
	V get(M message);

	void set(M message, V value);
}
