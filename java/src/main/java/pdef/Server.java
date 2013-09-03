package pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import pdef.rest.RestRequest;
import pdef.rest.RestResponse;
import pdef.rest.RestServer;
import pdef.rest.ServletRestServer;

import static com.google.common.base.Preconditions.checkNotNull;

/** Pdef server constructors. */
public class Server {
	private Server() {}

	/** Creates a rest server. */
	public static <T> Function<RestRequest, RestResponse> restServer(final Class<T> cls,
			final T service) {
		checkNotNull(cls);
		checkNotNull(service);

		return new RestServer<T>(cls, Suppliers.ofInstance(service));
	}

	/** Creates a rest server. */
	public static <T> RestServer<T> restServer(final Class<T> cls,
			final Supplier<T> serviceSupplier) {
		checkNotNull(cls);
		checkNotNull(serviceSupplier);

		return new RestServer<T>(cls, serviceSupplier);
	}

	/** Creates a servlet rest server. */
	public static <T> ServletRestServer<T> servletRestServer(
			final Class<T> cls, final T service) {
		Function<RestRequest, RestResponse> restServer = restServer(cls, service);
		return servletRestServer(restServer);
	}

	/** Creates a servlet rest server. */
	public static <T> ServletRestServer<T> servletRestServer(final Class<T> cls,
			final Supplier<T> serviceSupplier) {
		Function<RestRequest, RestResponse> restServer = restServer(cls, serviceSupplier);
		return servletRestServer(restServer);
	}

	/** Creates a servlet rest server. */
	public static <T> ServletRestServer<T> servletRestServer(
			final Function<RestRequest, RestResponse> restServer) {
		return new ServletRestServer<T>(restServer);
	}
}
