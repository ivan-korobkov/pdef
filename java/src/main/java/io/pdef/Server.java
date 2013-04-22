package io.pdef;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.descriptors.MethodDescriptor;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Server<T> {
	private final T service;

	public Server(final T service) {
		this.service = checkNotNull(service);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(service)
				.toString();
	}

	public ListenableFuture<?> handle(List<Invocation> invocations) {
		checkNotNull(invocations);
		checkArgument(!invocations.isEmpty());

		return (ListenableFuture<?>) doDispatch(service, invocations);
	}

	private Object doDispatch(final T service, final List<Invocation> invocations) {
		checkNotNull(service);
		checkNotNull(invocations);

		Object object = service;
		Iterator<Invocation> iterator = invocations.iterator();
		while (iterator.hasNext()) {
			if (object instanceof Dispatchable) {
				return ((Dispatchable) object).dispatch(ImmutableList.copyOf(iterator));
			}

			Invocation invocation = iterator.next();
			MethodDescriptor method = invocation.getMethod();
			Object[] args = invocation.getArgs();
			object = method.invoke(object, args);
		}

		return object;
	}
}