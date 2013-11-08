package io.pdef.rest;

import io.pdef.Invocation;
import io.pdef.Provider;
import io.pdef.Providers;
import io.pdef.descriptors.DataTypeDescriptor;
import io.pdef.descriptors.InterfaceDescriptor;

public class RestHandler<T> {
	private final InterfaceDescriptor<T> descriptor;
	private final Provider<T> provider;
	private final RestProtocol protocol;

	public RestHandler(final InterfaceDescriptor<T> descriptor, final T service) {
		this(descriptor, Providers.ofInstance(service));
	}

	public RestHandler(final InterfaceDescriptor<T> descriptor, final Provider<T> provider) {
		if (descriptor == null) throw new NullPointerException("descriptor");
		if (provider == null) throw new NullPointerException("provider");

		this.descriptor = descriptor;
		this.provider = provider;
		protocol = new RestProtocol();
	}

	@SuppressWarnings("unchecked")
	public RestResult<?> handle(final RestRequest request) throws Exception {
		if (request == null) throw new NullPointerException("request");

		Invocation invocation = protocol.getInvocation(request, descriptor);
		T service = provider.get();

		try {
			Object result = invocation.invoke(service);
			DataTypeDescriptor<Object> resultDescriptor =
					(DataTypeDescriptor<Object>) invocation.getResult();
			return RestResult.ok(result, resultDescriptor);

		} catch (Exception e) {
			DataTypeDescriptor<Exception> excDescriptor =
					(DataTypeDescriptor<Exception>) invocation.getExc();
			if (excDescriptor != null
					&& excDescriptor.getJavaClass().isAssignableFrom(e.getClass())) {
				// It's an application exception.
				return RestResult.exc(e, excDescriptor);
			}

			throw e;
		}
	}

	public RestServlet<T> servlet() {
		return new RestServlet<T>(this);
	}
}
