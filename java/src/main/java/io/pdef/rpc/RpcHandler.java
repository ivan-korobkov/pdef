package io.pdef.rpc;

import io.pdef.Invocation;
import io.pdef.Provider;
import io.pdef.Providers;
import io.pdef.descriptors.ValueDescriptor;
import io.pdef.descriptors.InterfaceDescriptor;

public class RpcHandler<T> {
	private final InterfaceDescriptor<T> descriptor;
	private final Provider<T> provider;
	private final RpcProtocol protocol;

	public RpcHandler(final InterfaceDescriptor<T> descriptor, final T service) {
		this(descriptor, Providers.ofInstance(service));
	}

	public RpcHandler(final InterfaceDescriptor<T> descriptor, final Provider<T> provider) {
		if (descriptor == null) throw new NullPointerException("descriptor");
		if (provider == null) throw new NullPointerException("provider");

		this.descriptor = descriptor;
		this.provider = provider;
		protocol = new RpcProtocol();
	}

	@SuppressWarnings("unchecked")
	public RpcResult<?> handle(final RpcRequest request) throws Exception {
		if (request == null) throw new NullPointerException("request");

		Invocation invocation = protocol.getInvocation(request, descriptor);
		T service = provider.get();

		try {
			Object result = invocation.invoke(service);
			ValueDescriptor<Object> resultDescriptor =
					(ValueDescriptor<Object>) invocation.getResult();
			return RpcResult.ok(result, resultDescriptor);

		} catch (Exception e) {
			ValueDescriptor<Exception> excDescriptor =
					(ValueDescriptor<Exception>) invocation.getExc();
			if (excDescriptor != null
					&& excDescriptor.getJavaClass().isAssignableFrom(e.getClass())) {
				// It's an application exception.
				return RpcResult.exc(e, excDescriptor);
			}

			throw e;
		}
	}

	public RpcServlet<T> servlet() {
		return new RpcServlet<T>(this);
	}
}
