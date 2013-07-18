package io.pdef.rpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import io.pdef.Invocation;
import io.pdef.InvocationResult;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.func.FluentFilter;

import java.util.List;

/** {@code RpcClient} converts invocations into rpc requests, passes them to an rpc sender,
 * and returns deserialized results from rpc responses.  */
public class RpcClient {
	private RpcClient() {}

	/** Converts an invocation into an rpc request, passes it to a sender,
	 * and returns a deserialized result from an rpc response. */
	public static InvocationResult apply(final Invocation invocation,
			final Function<RpcRequest, RpcResponse> sender) {
		checkNotNull(invocation);
		checkNotNull(sender);

		RpcRequest request = request(invocation);
		RpcResponse response = sender.apply(request);
		if (response == null) throw RpcErrors.clientError("Null response");

		boolean success = false;
		Descriptor<?> descriptor = null;
		MethodDescriptor method = invocation.getMethod();
		switch (response.getStatus()) {
			case OK:
				success = true;
				descriptor = method.getResult();
				break;
			case EXCEPTION:
				success = false;
				descriptor = method.getExc();
				break;
			case ERROR:
				throw RpcError.parse(response.getResult());
		}

		assert descriptor != null;
		Object parsedResult = descriptor.parse(response.getResult());

		return success ? InvocationResult.success(parsedResult, method)
		               : InvocationResult.exc(parsedResult, method);
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
	public static FluentFilter<Invocation, InvocationResult, RpcRequest, RpcResponse> filter() {
		return new FluentFilter<Invocation, InvocationResult, RpcRequest, RpcResponse>() {
			@Override
			public String toString() {
				return Objects.toStringHelper(RpcClient.class)
						.addValue(this)
						.toString();
			}

			@Override
			public InvocationResult apply(final Invocation remote,
					final Function<RpcRequest, RpcResponse> next) {
				return RpcClient.apply(remote, next);
			}
		};
	}

	/** Creates a function with a given sender, imitates currying. */
	public static Function<Invocation, InvocationResult> function(
			final Function<RpcRequest, RpcResponse> sender) {
		checkNotNull(sender);
		return new Function<Invocation, InvocationResult>() {
			@Override
			public String toString() {
				return Objects.toStringHelper(RpcClient.class)
						.addValue(this)
						.toString();
			}

			@Override
			public InvocationResult apply(final Invocation input) {
				return RpcClient.apply(input, sender);
			}
		};
	}
}
