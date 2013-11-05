package io.pdef.rest;

import io.pdef.*;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.invoke.Invoker;
import io.pdef.invoke.Invokers;

public class RestServer<T> implements RestHandler {
	private final InterfaceDescriptor<T> descriptor;
	private final Invoker invoker;
	private final RestProtocol protocol;

	private RestServer(final Class<T> cls, final Invoker invoker) {
		if (cls == null) throw new NullPointerException("cls");
		if (invoker == null) throw new NullPointerException("invoker");

		this.descriptor = Descriptors.findInterfaceDescriptor(cls);
		this.invoker = invoker;
		protocol = new RestProtocol();

		if (descriptor == null) {
			throw new IllegalArgumentException("Cannot find an interface descriptor in " + cls);
		}
	}

	public static <T> RestServer<T> create(final Class<T> cls, final T service) {
		return create(cls, Invokers.of(service));
	}

	public static <T> RestServer<T> create(final Class<T> cls, final Provider<T> serviceProvider) {
		return create(cls, Invokers.of(serviceProvider));
	}

	public static <T> RestServer<T> create(final Class<T> cls, final Invoker invoker) {
		return new RestServer<T>(cls, invoker);
	}

	@Override
	public RestResponse handle(final RestRequest request) throws Exception {
		if (request == null) throw new NullPointerException("request");

		Invocation invocation;
		try {
			invocation = protocol.parseInvocation(request, descriptor);
		} catch (RestException e) {
			return errorResponse(e);
		}

		DataDescriptor<?> dataDescriptor = invocation.getDataResult();
		MessageDescriptor<?> excDescriptor = invocation.getExc();

		InvocationResult result = invoker.invoke(invocation);
		return protocol.serializeInvocationResult(result, dataDescriptor, excDescriptor);
	}

	// VisibleForTesting
	RestResponse errorResponse(final RestException e) {
		return new RestResponse()
				.setStatus(e.getStatus())
				.setContent(e.getMessage())
				.setTextContentType();
	}
}
