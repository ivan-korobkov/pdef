package io.pdef;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.pdef.rpc.*;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

class ClientRpcProtocol implements Function<Invocation, Object> {
	private final Function<Request, Response> rpcSender;

	ClientRpcProtocol(final Function<Request, Response> rpcSender) {
		this.rpcSender = checkNotNull(rpcSender);
	}

	@Override
	public Object apply(final Invocation remote) {
		return handle(remote, rpcSender);
	}

	static Object handle(final Invocation remote, final Function<Request, Response> rpcHandler) {
		checkNotNull(remote);
		Request request = createRequest(remote);
		Response response = rpcHandler.apply(request);
		if (response == null) throw RpcError.builder()
			.setCode(RpcErrorCode.CLIENT_ERROR)
			.setText("Null response")
			.build();

		Object result = response.getResult();
		switch (response.getStatus()) {
			case OK:
				return remote.getResult().parse(result);
			case EXCEPTION:
				throw (RuntimeException) remote.getExc().parse(result);
			case ERROR:
				throw RpcError.parse(result);
		}

		throw new AssertionError();
	}

	/** Serializes a remote proxy into an rpc request. */
	static Request createRequest(final Invocation remote) {
		checkArgument(remote.isRemote(), "must be a remote invocation, got %s", remote);
		List<Invocation> invocations = remote.toList();

		List<MethodCall> calls = Lists.newArrayList();
		for (Invocation invocation : invocations) calls.add(invocation.serialize());

		return Request.builder()
				.setCalls(calls)
				.build();
	}

	static class Filter extends AbstractFilter<Invocation, Object, Request, Response> {
		@Override
		public Object apply(final Invocation input, final Function<Request, Response> next) {
			return handle(input, next);
		}
	}
}
