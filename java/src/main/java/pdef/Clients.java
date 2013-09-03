package pdef;

import com.google.common.base.Function;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import pdef.descriptors.InterfaceDescriptor;
import pdef.rest.RestClient;
import pdef.rest.RestRequest;
import pdef.rest.RestResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Pdef client constructors. */
public class Clients {
	private Clients() {}

	/** Creates a client interface proxy. */
	public static <T> T proxy(final Class<T> cls, final Function<Invocation, Object> client) {
		checkNotNull(cls);
		checkNotNull(client);

		InterfaceDescriptor descriptor = InterfaceDescriptor.findDescriptor(cls);
		checkArgument(descriptor != null, "Cannot find an interface descriptor in " + cls);

		return ClientProxy.create(cls, descriptor, client);
	}

	/** Creates a rest client with the default http sender and a base url. */
	public static <T> T restClient(final Class<T> cls, final String url) {
		checkNotNull(cls);
		checkNotNull(url);

		RestClient client = new RestClient(url);
		return proxy(cls, client);
	}

	/** Creates a rest client. */
	public static <T> T restClient(final Class<T> cls, final String url,
			final Function<Request, Response> httpSession) {
		checkNotNull(cls);
		checkNotNull(url);
		checkNotNull(httpSession);

		RestClient client = new RestClient(url, httpSession);
		return proxy(cls, client);
	}

	/** Creates a rest client with a custom sender. */
	public static <T> T restClient(final Class<T> cls,
			final Function<RestRequest, RestResponse> sender) {
		checkNotNull(cls);
		checkNotNull(sender);

		RestClient client = new RestClient(sender);
		return proxy(cls, client);
	}
}
