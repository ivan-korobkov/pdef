package io.pdef.rest;

import io.pdef.Func;
import io.pdef.Provider;
import io.pdef.Providers;
import io.pdef.descriptors.DataDescriptor;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MessageDescriptor;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.invoke.Invoker;

import javax.servlet.http.HttpServlet;

public class RestServer<T> implements Func<RestRequest, RestResponse> {
	private final InterfaceDescriptor<T> descriptor;
	private final Func<Invocation, InvocationResult> invoker;
	private final RestProtocol format;

	/** Creates a REST server handler. */
	public RestServer(final Class<T> cls, final Func<Invocation, InvocationResult> invoker) {
		if (cls == null) throw new NullPointerException("cls");
		if (invoker == null) throw new NullPointerException("invoker");

		this.descriptor = InterfaceDescriptor.findDescriptor(cls);
		this.invoker = invoker;
		format = new RestProtocol();

		if (descriptor == null) {
			throw new IllegalArgumentException("Cannot find an interface descriptor in " + cls);
		}
	}

	@Override
	public RestResponse apply(final RestRequest request) throws Exception {
		if (request == null) throw new NullPointerException("request");

		Invocation invocation;
		try {
			invocation = format.parseInvocation(request, descriptor);
		} catch (RestException e) {
			return errorResponse(e);
		}

		DataDescriptor<?> dataDescriptor = invocation.getDataResult();
		MessageDescriptor<?> excDescriptor = invocation.getExc();

		InvocationResult result = invoker.apply(invocation);
		return format.serializeInvocationResult(result, dataDescriptor, excDescriptor);
	}

	// VisibleForTesting
	RestResponse errorResponse(final RestException e) {
		return new RestResponse()
				.setStatus(e.getStatus())
				.setContent(e.getMessage())
				.setTextContentType();
	}

	public static <T> Builder<T> builder(final Class<T> cls) {
		return new Builder<T>().setCls(cls);
	}

	public static class Builder<T> {
		private Class<T> cls;
		private Provider<T> serviceProvider;
		private Func<Invocation, InvocationResult> invoker;

		public Class<T> getCls() {
			return cls;
		}

		public Builder<T> setCls(final Class<T> cls) {
			this.cls = cls;
			return this;
		}

		public Provider<T> getServiceProvider() {
			return serviceProvider;
		}

		public Builder<T> setServiceProvider(final Provider<T> serviceProvider) {
			if (invoker != null) {
				throw new IllegalStateException(
						"Cannot set a service provider, an invoker is already present");
			}
			this.serviceProvider = serviceProvider;
			return this;
		}

		public Builder<T> setService(final T service) {
			if (service == null) throw new NullPointerException("service");
			return setServiceProvider(Providers.ofInstance(service));
		}

		public Func<Invocation, InvocationResult> getInvoker() {
			return invoker;
		}

		public Builder<T> setInvoker(final Func<Invocation, InvocationResult> invoker) {
			if (serviceProvider != null) {
				throw new IllegalStateException(
						"Cannot set an invoker, a service provider is already present");
			}
			this.invoker = invoker;
			return this;
		}

		private Func<Invocation, InvocationResult> buildInvoker() {
			if (serviceProvider == null && invoker == null) {
				throw new IllegalStateException(
						"Service/serviceProvider or invoker must be present");
			}
			if (serviceProvider != null) {
				return Invoker.of(serviceProvider);
			} else {
				return invoker;
			}
		}

		/** Creates a raw REST server. */
		public RestServer<T> build() {
			if (cls == null) {
				throw new IllegalStateException("Interface class must be set");
			}

			Func<Invocation, InvocationResult> invoker = buildInvoker();
			return new RestServer<T>(cls, invoker);
		}

		/** Creates a servlet REST handler. */
		public HttpServlet buildServlet() {
			RestServer<T> server = build();
			return new HttpRestServlet(server);
		}
	}
}
