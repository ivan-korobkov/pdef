package io.pdef;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.raw.RawParser;
import io.pdef.raw.RawSerializer;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.transform;

public class Server<T> {
	private final T service;
	private final Class<T> cls;
	private final Parser parser;
	private final Serializer serializer;

	public Server(final T service, final Class<T> cls, final DescriptorPool pool) {
		this.service = checkNotNull(service);
		this.cls = checkNotNull(cls);
		parser = new RawParser(pool);
		serializer = new RawSerializer(pool);
	}

	public ListenableFuture<?> handle(Object request) {
		List<Invocation> invocations = parser.parseInvocations(cls, request);
		checkArgument(!invocations.isEmpty());

		ListenableFuture<?> future = (ListenableFuture<?>) doDispatch(service, invocations);
		return transform(future, new ResultFunction());
	}

	private Object onResult(final Object input) {
		return serializer.serialize(input);
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
			List<?> args = invocation.getArgs();
			object = method.invoke(object, args);
		}

		return object;
	}

	private class ResultFunction implements Function<Object, Object> {
		@Nullable
		@Override
		public Object apply(@Nullable final Object input) {
			return onResult(input);
		}
	}
}
