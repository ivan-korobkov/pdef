package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerInvocationHandler<T> implements Function<Invocation, Object> {
	private final Supplier<T> supplier;

	private ServerInvocationHandler(final Supplier<T> supplier) {
		this.supplier = checkNotNull(supplier);
	}

	/** Creates an invocation handler from a service instance. */
	public static <T> Function<Invocation, Object> create(final T service) {
		checkNotNull(service);
		return new ServerInvocationHandler<T>(Suppliers.ofInstance(service));
	}

	/** Creates an invocation handler from a service supplier. */
	public static <T> Function<Invocation, Object> create(final Supplier<T> supplier) {
		return new ServerInvocationHandler<T>(supplier);
	}

	@Override
	public Object apply(final Invocation invocation) {
		Object object = supplier.get();
		return invocation.invokeChainOn(object);
	}
}
