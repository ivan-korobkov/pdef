package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

class ServerInvocationHandler<T> implements Function<Invocation, Object> {
	private final Supplier<T> supplier;

	ServerInvocationHandler(final Supplier<T> supplier) {
		this.supplier = checkNotNull(supplier);
	}

	@Override
	public Object apply(final Invocation invocation) {
		Object object = supplier.get();
		return invocation.invokeChainOn(object);
	}
}
