package io.pdef.server;

import com.google.common.base.Function;
import io.pdef.invocation.RemoteInvocation;
import io.pdef.rpc.Request;

import static com.google.common.base.Preconditions.checkNotNull;

/** Reads a remote invocation from a request. */
public class RequestToInvocation implements Function<Request, RemoteInvocation> {
	private final Class<?> iface;

	public RequestToInvocation(final Class<?> iface) {
		this.iface = checkNotNull(iface);
	}

	@Override
	public RemoteInvocation apply(final Request input) {
		checkNotNull(input);
		return null;
	}
}
