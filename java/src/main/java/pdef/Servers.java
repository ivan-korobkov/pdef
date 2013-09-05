package pdef;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import pdef.rest.RestRequest;
import pdef.rest.RestResponse;
import pdef.rest.RestServer;
import pdef.rest.RestServerHandler;

/** Pdef server constructors. */
public class Servers {
	private Servers() {}

	/** Creates a default REST server. */
	public static <T> RestServer server(final Class<T> cls, final T service) {
		checkNotNull(cls);
		checkNotNull(service);

		return server(cls, Suppliers.ofInstance(service));
	}

	/** Creates a default REST server. */
	public static <T> RestServer server(final Class<T> cls, final Supplier<T> serviceSupplier) {
		checkNotNull(cls);
		checkNotNull(serviceSupplier);

		Invoker<T> invoker = invoker(serviceSupplier);
		RestServerHandler handler = handler(cls, invoker);
		return server(handler);
	}

	/** Creates a REST server with a custom handler. */
	public static RestServer server(final Function<RestRequest, RestResponse> handler) {
		checkNotNull(handler);

		return new RestServer(handler);
	}

	/** Creates a REST handler with a custom invoker. */
	public static RestServerHandler handler(final Class<?> cls,
			final Function<Invocation, Object> invoker) {
		checkNotNull(cls);
		checkNotNull(invoker);

		return new RestServerHandler(cls, invoker);
	}

	/** Creates a service invoker. */
	public static <T> Invoker<T> invoker(final Supplier<T> serviceSupplier) {
		checkNotNull(serviceSupplier);

		return new Invoker<T>(serviceSupplier);
	}
}
