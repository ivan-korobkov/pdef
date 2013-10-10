package io.pdef.types;

public interface MessageFieldAccessor<M, V>
		extends MessageFieldGetter<M, V>, MessageFieldSetter<M, V> {}
