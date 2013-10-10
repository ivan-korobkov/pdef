package io.pdef.types;

public interface MessageFieldGetter<M, V> {
	V get(M message);
}
