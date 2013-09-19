package pdef.invocation;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import static com.google.common.base.Preconditions.checkNotNull;

public class Invoker<T> implements Function<Invocation, InvocationResult> {
	private final Supplier<T> serviceSupplier;

	public static <T> Invoker<T> of(final T service) {
		checkNotNull(service);
		return of(Suppliers.<T>ofInstance(service));
	}

	public static <T> Invoker<T> of(final Supplier<T> supplier) {
		checkNotNull(supplier);
		return new Invoker<T>(supplier);
	}

	Invoker(final Supplier<T> serviceSupplier) {
		this.serviceSupplier = checkNotNull(serviceSupplier);
	}

	@Override
	public InvocationResult apply(final Invocation invocation) {
		T service = serviceSupplier.get();
		return invocation.invoke(service);
	}
}
