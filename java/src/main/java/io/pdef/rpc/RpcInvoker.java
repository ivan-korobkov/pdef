package io.pdef.rpc;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import io.pdef.Invocation;

import static com.google.common.base.Preconditions.checkNotNull;

public class RpcInvoker<T> implements Function<Invocation, Object> {
	private final Supplier<T> supplier;

	RpcInvoker(final Supplier<T> supplier) {
		this.supplier = checkNotNull(supplier);
	}

	/** Creates an invoker from a service instance. */
	public static <T> Function<Invocation, Object> function(final T service) {
		checkNotNull(service);
		return new RpcInvoker<T>(Suppliers.ofInstance(service));
	}

	/** Creates an invoker from a service supplier. */
	public static <T> Function<Invocation, Object> function(final Supplier<T> supplier) {
		return new RpcInvoker<T>(supplier);
	}

	@Override
	public Object apply(final Invocation invocation) {
		Object object = supplier.get();
		return invocation.invokeChainOn(object);
	}
}
