package io.pdef;

public interface InvocationHandler {
	<T> T apply(Invocation invocation);
}
