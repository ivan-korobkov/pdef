package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.invoke.Invoker;
import io.pdef.rest.RestRequest;
import io.pdef.rest.RestResponse;
import io.pdef.rest.RestServerReceiver;
import io.pdef.rest.RestServerHandler;

import static com.google.common.base.Preconditions.checkNotNull;

/** Pdef server constructors. */
public class Servers {
	private Servers() {}

	/** Creates a default REST server. */
	public static <T> RestServerReceiver server(final Class<T> cls, final T service) {
		checkNotNull(cls);
		checkNotNull(service);

		return server(cls, Suppliers.ofInstance(service));
	}

	/** Creates a default REST server. */
	public static <T> RestServerReceiver server(final Class<T> cls, final Supplier<T> serviceSupplier) {
		checkNotNull(cls);
		checkNotNull(serviceSupplier);

		Invoker<T> invoker = invoker(serviceSupplier);
		RestServerHandler handler = handler(cls, invoker);
		return server(handler);
	}

	/** Creates a REST server with a custom handler. */
	public static RestServerReceiver server(final Function<RestRequest, RestResponse> handler) {
		checkNotNull(handler);
		return new RestServerReceiver(handler);
	}

	/** Creates a REST handler with a custom invoker. */
	public static RestServerHandler handler(final Class<?> cls,
			final Function<Invocation, InvocationResult> invoker) {
		checkNotNull(cls);
		checkNotNull(invoker);
		return new RestServerHandler(cls, invoker);
	}

	/** Creates a service invoker. */
	public static <T> Invoker<T> invoker(final Supplier<T> serviceSupplier) {
		checkNotNull(serviceSupplier);
		return Invoker.of(serviceSupplier);
	}
}
