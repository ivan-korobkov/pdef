package io.pdef.server;

import com.google.common.base.Function;
import io.pdef.invocation.RemoteInvocation;

import static com.google.common.base.Preconditions.checkNotNull;

/** Handles a remote invocation and returns the result. */
public class InvocationToResult implements Function<RemoteInvocation, Object> {
	private final Object service;

	public InvocationToResult(final Object service) {
		this.service = checkNotNull(service);
	}

	@Override
	public Object apply(final RemoteInvocation input) {
		return null;
	}
}
