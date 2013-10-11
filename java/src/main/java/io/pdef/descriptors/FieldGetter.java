package io.pdef.descriptors;

public interface FieldGetter<M, V> {
	V get(M message);
}
