package io.pdef.rpc;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.Dispatchable;
import io.pdef.Invocation;
import io.pdef.descriptors.MethodDescriptor;

import java.util.Iterator;
import java.util.List;

public class AsyncDispatcher<T> implements Func<List<Invocation>, ListenableFuture<Object>> {
	private final T service;

	public AsyncDispatcher(final T service) {
		this.service = checkNotNull(service);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(service)
				.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ListenableFuture<Object> apply(final List<Invocation> invocations) {
		checkNotNull(invocations);
		checkArgument(!invocations.isEmpty());

		return (ListenableFuture<Object>) doDispatch(service, invocations);
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
