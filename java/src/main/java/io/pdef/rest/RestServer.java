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

public class RestServer<T> implements Function<RestRequest, RestResponse> {
	private final InterfaceDescriptor<T> descriptor;
	private final Function<Invocation, InvocationResult> invoker;
	private final RestFormat format;

	/** Creates a default REST server. */
	public static <T> RestServerServletHandler servletServer(final Class<T> cls, final T service) {
		checkNotNull(cls);
		checkNotNull(service);

		return servletServer(cls, Suppliers.ofInstance(service));
	}

	/** Creates a default REST server. */
	public static <T> RestServerServletHandler servletServer(final Class<T> cls,
			final Supplier<T> serviceSupplier) {
		checkNotNull(cls);
		checkNotNull(serviceSupplier);

		Invoker<T> invoker = Invoker.of(serviceSupplier);
		RestServer<T> restServer = new RestServer<T>(cls, invoker);
		return new RestServerServletHandler(restServer);
	}

	public static <T> RestServer<T> create(final Class<T> cls,
			final Function<Invocation, InvocationResult> invoker) {
		return new RestServer<T>(cls, invoker);
	}

	/** Creates a REST server handler. */
	public RestServer(final Class<T> cls, final Function<Invocation, InvocationResult> invoker) {
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
}
