package io.pdef.invoke;

import io.pdef.Func;

import io.pdef.Provider;
import io.pdef.Providers;


public class Invoker<T> implements Func<Invocation, InvocationResult> {
	private final Provider<T> serviceProvider;

	public static <T> Invoker<T> of(final T service) {
		if (service == null) throw new NullPointerException("service");
		return of(Providers.<T>ofInstance(service));
	}

	public static <T> Invoker<T> of(final Provider<T> provider) {
		return new Invoker<T>(provider);
	}

	Invoker(final Provider<T> serviceProvider) {
		if (serviceProvider == null) throw new NullPointerException("provider");
		this.serviceProvider = serviceProvider;
	}

	@Override
	public InvocationResult apply(final Invocation invocation) {
		T service = serviceProvider.get();
		return invocation.invoke(service);
	}
}
