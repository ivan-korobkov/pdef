package io.pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import io.pdef.descriptors.DataDescriptor;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MessageDescriptor;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.invoke.Invoker;

import javax.servlet.http.HttpServlet;

public class RestServer<T> implements Function<RestRequest, RestResponse> {
	private final InterfaceDescriptor<T> descriptor;
	private final Function<Invocation, InvocationResult> invoker;
	private final RestFormat format;

	/** Creates a REST server handler. */
	private RestServer(final Class<T> cls, final Function<Invocation, InvocationResult> invoker) {
		this.descriptor = InterfaceDescriptor.findDescriptor(cls);
		this.invoker = checkNotNull(invoker);
		format = new RestFormat();

		checkArgument(descriptor != null, "Cannot find an interface descriptor in %s", cls);
	}

	@Override
	public RestResponse apply(final RestRequest request) {
		checkNotNull(request);

		Invocation invocation;
		try {
			invocation = format.parseInvocation(request, descriptor);
		} catch (RestException e) {
			return errorResponse(e);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		try {
			DataDescriptor<?> dataDescriptor = invocation.getDataResult();
			MessageDescriptor<?> excDescriptor = invocation.getExc();

			InvocationResult result = invoker.apply(invocation);
			return format.serializeInvocationResult(result, dataDescriptor, excDescriptor);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	@VisibleForTesting
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
		private Supplier<T> serviceSupplier;
		private Function<Invocation, InvocationResult> invoker;

		public Class<T> getCls() {
			return cls;
		}

		public Builder<T> setCls(final Class<T> cls) {
			this.cls = cls;
			return this;
		}

		public Supplier<T> getServiceSupplier() {
			return serviceSupplier;
		}

		public Builder<T> setServiceSupplier(final Supplier<T> serviceSupplier) {
			checkState(invoker == null,
					"Cannot set a service supplier, an invoker is already present");
			this.serviceSupplier = serviceSupplier;
			return this;
		}

		public Builder<T> setService(final T service) {
			checkNotNull(service);
			return setServiceSupplier(Suppliers.ofInstance(service));
		}

		public Function<Invocation, InvocationResult> getInvoker() {
			return invoker;
		}

		public Builder<T> setInvoker(final Function<Invocation, InvocationResult> invoker) {
			checkState(serviceSupplier == null,
					"Cannot set an invoker, a service supplier is already present");
			this.invoker = invoker;
			return this;
		}

		private Function<Invocation, InvocationResult> buildInvoker() {
			checkState(serviceSupplier != null || invoker != null,
					"Service/serviceSupplier or invoker must be present");
			if (serviceSupplier != null) {
				return Invoker.of(serviceSupplier);
			} else {
				return invoker;
			}
		}

		/** Creates a raw REST server. */
		public RestServer<T> build() {
			checkState(cls != null, "Interface class must be set");
			Function<Invocation, InvocationResult> invoker = buildInvoker();
			return new RestServer<T>(cls, invoker);
		}

		/** Creates a servlet REST handler. */
		public HttpServlet buildServlet() {
			RestServer<T> server = build();
			return new RestServerServlet(server);
		}
	}
}
