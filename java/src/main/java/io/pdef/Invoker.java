package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import static com.google.common.base.Preconditions.checkNotNull;

public class Invoker<T> implements Function<Invocation, InvocationResult> {
	private final Supplier<T> supplier;

	Invoker(final Supplier<T> supplier) {
		this.supplier = checkNotNull(supplier);
	}

	/** Creates an invoker from a service instance. */
	public static <T> Function<Invocation, InvocationResult> from(final T service) {
		checkNotNull(service);
		return new Invoker<T>(Suppliers.ofInstance(service));
	}

	/** Creates an invoker from a service supplier. */
	public static <T> Function<Invocation, InvocationResult> from(final Supplier<T> supplier) {
		return new Invoker<T>(supplier);
	}

	@Override
	public InvocationResult apply(final Invocation invocation) {
		Object object = supplier.get();
		return invocation.invokeChainOn(object);
	}
}
