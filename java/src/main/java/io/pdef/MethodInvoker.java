package io.pdef;

public interface MethodInvoker<T, R> {
	/**
	 * Invokes a method on an object with a given arguments.
	 */
	R invoke(T object, Object[] args) throws Exception;
}
