package io.pdef.meta;

public interface MessageFieldSetter<M, V> {
	void set(M message, V value);
}
