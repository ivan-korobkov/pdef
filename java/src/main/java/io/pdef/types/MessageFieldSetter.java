package io.pdef.types;

public interface MessageFieldSetter<M, V> {
	void set(M message, V value);
}
