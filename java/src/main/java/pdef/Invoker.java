package pdef;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;

public class Invoker<T> implements Function<Invocation, InvocationResult> {
	private final Supplier<T> serviceSupplier;

	Invoker(final Supplier<T> serviceSupplier) {
		this.serviceSupplier = checkNotNull(serviceSupplier);
	}

	@Override
	public InvocationResult apply(final Invocation invocation) {
		T service = serviceSupplier.get();
		return invocation.invoke(service);
	}
}
