package io.pdef.rpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import io.pdef.Invocation;
import io.pdef.func.FluentFilter;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** {@code RpcClient} converts invocations into rpc requests, passes them to an rpc sender,
 * and returns deserialized results from rpc responses.  */
public class RpcClient {
	private RpcClient() {}

	/** Converts an invocation into an rpc request, passes it to a sender,
	 * and returns a deserialized result from an rpc response. */
	public static Object apply(final Invocation remote, final Function<RpcRequest, RpcResponse> sender) {
		checkNotNull(remote);
		checkNotNull(sender);

		RpcRequest request = request(remote);
		RpcResponse response = sender.apply(request);
		if (response == null) throw RpcErrors.clientError("Null response");

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
	@VisibleForTesting
	static RpcRequest request(final Invocation remote) {
		checkArgument(remote.isRemote(), "must be a remote invocation, got %s", remote);
		List<Invocation> invocations = remote.toList();

		List<RpcCall> calls = Lists.newArrayList();
		for (Invocation invocation : invocations) calls.add(invocation.serialize());

		return RpcRequest.builder()
				.setCalls(calls)
				.build();
	}

	/** Creates a fluent filter. */
	public static FluentFilter<Invocation, Object, RpcRequest, RpcResponse> filter() {
		return new FluentFilter<Invocation, Object, RpcRequest, RpcResponse>() {
			@Override
			public String toString() {
				return Objects.toStringHelper(RpcClient.class)
						.addValue(this)
						.toString();
			}

			@Override
			public Object apply(final Invocation remote, final Function<RpcRequest, RpcResponse> next) {
				return RpcClient.apply(remote, next);
			}
		};
	}

	/** Creates a function with a given sender, imitates currying. */
	public static Function<Invocation, Object> function(final Function<RpcRequest, RpcResponse> sender) {
		checkNotNull(sender);
		return new Function<Invocation, Object>() {
			@Override
			public String toString() {
				return Objects.toStringHelper(RpcClient.class)
						.addValue(this)
						.toString();
			}

			@Override
			public Object apply(final Invocation input) {
				return RpcClient.apply(input, sender);
			}
		};
	}
}
