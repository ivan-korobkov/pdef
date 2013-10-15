package io.pdef.invoke;

import io.pdef.Func;
import io.pdef.Provider;
import io.pdef.Providers;

public class Invoker<T> implements Func<Invocation, InvocationResult> {
	private final Provider<T> provider;

	public static <T> Invoker<T> of(final T service) {
		if (service == null) throw new NullPointerException("service");
		return new Invoker<T>(Providers.ofInstance(service));
	}

	public static <T> Invoker<T> of(final Provider<T> provider) {
		if (provider == null) throw new NullPointerException("provider");
		return new Invoker<T>(provider);
	}

	private Invoker(final Provider<T> provider) {
		this.provider = provider;
	}

	@Override
	public InvocationResult apply(final Invocation invocation) throws RuntimeException {
		T service = provider.get();
		return invocation.invoke(service);
	}
}
