package io.pdef.client;

import io.pdef.invocation.InvocationFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.google.common.base.Preconditions.checkNotNull;

public class InvocationProxy<T> implements InvocationHandler {
	private final Class<T> iface;
	private final RemoteHandlerFactory handlerFactory;
	private final InvocationFactory invocationFactory;
	private Proxy proxy;

	public InvocationProxy(final Class<T> iface, final RemoteHandlerFactory handlerFactory,
			final InvocationFactory invocationFactory) {
		this.iface = checkNotNull(iface);
		this.handlerFactory = checkNotNull(handlerFactory);
		this.invocationFactory = checkNotNull(invocationFactory);
	}

	public T proxy() {
		return null;
	}

	@Override
	public Object invoke(final Object o, final Method method, final Object[] objects)
			throws Throwable {
		return null;
	}
}
