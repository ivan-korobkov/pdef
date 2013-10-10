package io.pdef.meta;

public interface MessageFieldGetter<M, V> {
	V get(M message);
}
