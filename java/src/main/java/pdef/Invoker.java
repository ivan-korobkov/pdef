package pdef;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;

public class Invoker<T> implements Function<Invocation, Object> {
	private final Supplier<T> serviceSupplier;

	Invoker(final Supplier<T> serviceSupplier) {
		this.serviceSupplier = checkNotNull(serviceSupplier);
	}

	@Override
	public Object apply(final Invocation invocation) {
		T service = serviceSupplier.get();
		try {
			return invocation.invoke(service);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
}
