package io.pdef.rpc;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import io.pdef.Invocation;
import io.pdef.InvocationResult;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.func.FluentFunction;

import java.util.List;

/** {@code RpcClient} converts invocations into rpc requests, passes them to an rpc sender,
 * and returns deserialized results from rpc responses.  */
public class RpcClient {
	private RpcClient() {}

	/** Creates an RPC request from a remote invocation. */
	public static RpcRequest writeRequest(final Invocation remote) {
		checkArgument(remote.isRemote(), "must be a remote invocation, got %s", remote);
		List<Invocation> invocations = remote.toList();

		List<RpcCall> calls = Lists.newArrayList();
		for (Invocation invocation : invocations) calls.add(invocation.serialize());

		return RpcRequest.builder()
				.setCalls(calls)
				.build();
	}

	/** Parses an RPC response into an invocation result.
	 *
	 * @throws RpcError if the response is an error response. */
	public static InvocationResult readResponse(final RpcResponse response,
			final MethodDescriptor method) {
		if (response == null) throw RpcErrors.clientError("Null response");

		boolean success = false;
		Descriptor<?> descriptor = null;
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

	/** Returns a RPC request writer. */
	public static FluentFunction<Invocation, RpcRequest> requestWriter() {
		return new RequestWriter();
	}

	/** Returns an invocation RPC response reader. */
	public static FluentFunction<RpcResponse, InvocationResult> responseReader(
			final Invocation invocation) {
		return responseReader(invocation.getMethod());
	}

	/** Returns a method RPC response reader. */
	public static FluentFunction<RpcResponse, InvocationResult> responseReader(
			final MethodDescriptor method) {
		return new ResponseReader(method);
	}

	private static class RequestWriter extends FluentFunction<Invocation, RpcRequest> {
		@Override
		public RpcRequest apply(final Invocation input) {
			return writeRequest(input);
		}
	}

	private static class ResponseReader extends FluentFunction<RpcResponse, InvocationResult> {
		private final MethodDescriptor method;

		private ResponseReader(final MethodDescriptor method) {
			this.method = checkNotNull(method);
		}

		@Override
		public InvocationResult apply(final RpcResponse input) {
			return readResponse(input, method);
		}
	}
}
