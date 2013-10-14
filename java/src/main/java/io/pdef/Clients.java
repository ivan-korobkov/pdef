package io.pdef;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationProxy;
import io.pdef.invoke.InvocationResult;
import io.pdef.rest.RestClientHandler;
import io.pdef.rest.RestClientSender;
import io.pdef.rest.RestRequest;
import io.pdef.rest.RestResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import javax.annotation.Nullable;

/** Pdef client constructors. */
public class Clients {
	private Clients() {}

	/** Creates a default REST client. */
	public static <T> T client(final Class<T> cls, final String url) {
		checkNotNull(cls);
		checkNotNull(url);

		return client(cls, url, null);
	}

	/** Creates a default REST client with a custom session. */
	public static <T> T client(final Class<T> cls, final String url,
			@Nullable final Function<Request, Response> session) {
		checkNotNull(cls);
		checkNotNull(url);

		RestClientSender sender = sender(url, session);
		RestClientHandler handler = handler(sender);
		return client(cls, handler);
	}

	/** Creates a custom client. */
	public static <T> T client(final Class<T> cls,
			final Function<Invocation, InvocationResult> invocationHandler) {
		checkNotNull(cls);
		checkNotNull(invocationHandler);

		InterfaceDescriptor<T> descriptor = InterfaceDescriptor.findDescriptor(cls);
		checkArgument(descriptor != null, "Cannot find an interface descriptor in " + cls);

		return InvocationProxy.create(cls, descriptor, invocationHandler);
	}

	/** Creates a REST client invocation handler with a custom sender. */
	public static RestClientHandler handler(final Function<RestRequest, RestResponse> sender) {
		checkNotNull(sender);
		return new RestClientHandler(sender);
	}

	/** Creates a REST client sender. */
	public static RestClientSender sender(final String url) {
		checkNotNull(url);
		return sender(url, null);
	}

	/** Creates a REST client sender with a custom session or a default one. */
	public static RestClientSender sender(final String url,
			@Nullable final Function<Request, Response> session) {
		checkNotNull(url);
		return new RestClientSender(url, session);
	}
}
