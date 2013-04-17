package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.descriptors.FutureDescriptor;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.raw.RawParser;
import io.pdef.raw.RawSerializer;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.transform;

public class Client<T> {
	private final Class<T> cls;
	private final ClientRequestHandler requestHandler;

	private final Parser parser;
	private final Serializer serializer;
	private final InterfaceDescriptor descriptor;

	public Client(final Class<T> cls, final DescriptorPool pool,
			final ClientRequestHandler requestHandler) {
		this.cls = checkNotNull(cls);
		this.requestHandler = checkNotNull(requestHandler);

		parser = new RawParser(pool);
		serializer = new RawSerializer(pool);
		descriptor = (InterfaceDescriptor) pool.getDescriptor(cls);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(cls)
				.toString();
	}

	@SuppressWarnings("unchecked")
	public T proxy() {
		Invoker invoker = Invoker.of(descriptor, new InvocationHandler());
		return (T) invoker.toProxy();
	}

	private Object doHandle(final List<Invocation> invocations) {
		checkNotNull(invocations);
		checkArgument(!invocations.isEmpty());
		FutureDescriptor descriptor = (FutureDescriptor) invocations.get(invocations.size() - 1)
				.getMethod().getResult();

		Object request = serializer.serializeInvocations(invocations);
		ListenableFuture<?> future = requestHandler.handle(request);
		return transform(future, new ResultFunction(descriptor));
	}

	protected Object onResult(final FutureDescriptor descriptor, final Object result) {
		Type resultType = descriptor.getElementType();
		return parser.parse(resultType, result);
	}

	private class InvocationHandler implements InvocationListHandler {
		@Override
		public Object handle(final List<Invocation> invocations) {
			return doHandle(invocations);
		}
	}

	private class ResultFunction implements Function<Object, Object> {
		private final FutureDescriptor descriptor;

		private ResultFunction(final FutureDescriptor descriptor) {
			this.descriptor = checkNotNull(descriptor);
		}

		@Override
		public Object apply(@Nullable final Object input) {
			return onResult(descriptor, input);
		}
	}
}
