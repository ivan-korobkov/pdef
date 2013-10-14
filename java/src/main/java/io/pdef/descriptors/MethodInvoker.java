package io.pdef.descriptors;

public interface MethodInvoker<T, R> {
	R invoke(T object, Object[] args) throws Exception;
}
