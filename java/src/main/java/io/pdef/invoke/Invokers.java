package io.pdef.invoke;

import io.pdef.Provider;
import io.pdef.Providers;

public class Invokers {
	private Invokers() {}

	public static <T> Invoker of(final T service) {
		if (service == null) throw new NullPointerException("service");
		return new DefaultInvoker<T>(Providers.ofInstance(service));
	}

	public static <T> Invoker of(final Provider<T> provider) {
		if (provider == null) throw new NullPointerException("provider");
		return new DefaultInvoker<T>(provider);
	}

	private static class DefaultInvoker<T> implements Invoker {
		private final Provider<T> provider;

		private DefaultInvoker(final Provider<T> provider) {
			this.provider = provider;
		}

		@Override
		public InvocationResult invoke(final Invocation invocation) throws Exception {
			T service = provider.get();
			return invocation.invoke(service);
		}
	}
}
