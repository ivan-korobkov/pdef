package pdef.rpc;

import static com.google.common.base.Preconditions.*;
import pdef.Invocation;
import pdef.InvocationResult;
import pdef.descriptors.Descriptor;
import pdef.descriptors.InterfaceDescriptor;
import pdef.descriptors.MethodDescriptor;
import pdef.func.FluentFunction;

import java.util.List;

public class RpcServer {
	private RpcServer() {}

	/** Parses an interface invocation chain from an RPC request. */
	public static Invocation readRequest(final RpcRequest request,
			final InterfaceDescriptor<?> descriptor) {
		checkNotNull(request);
		List<RpcCall> calls = request.getCalls();
		if (calls.isEmpty()) throw RpcErrors.methodCallsRequired();

		StringBuilder path = new StringBuilder();
		InterfaceDescriptor<?> d = descriptor;
		Invocation invocation = Invocation.root();

		for (final RpcCall call : calls) {
			String name = call.getMethod();
			path.append(path.length() == 0 ? "" : ".").append(name);
			if (d == null) throw RpcErrors.methodNotFound(path);

			MethodDescriptor method = d.getMethods().get(name);
			if (method == null) throw RpcErrors.methodNotFound(path);

			try {
				invocation = method.parse(invocation, call.getArgs());
			} catch (Exception e) {
				throw RpcErrors.wrongMethodArgs(path);
			}

			if (!invocation.isRemote()) d = invocation.getNext();
		}

		if (!invocation.isRemote()) throw RpcErrors.notRemoteMethod(path);
		return invocation;
	}

	/** Creates an RPC response from an invocation result. */
	public static RpcResponse writeResponse(final InvocationResult result) {
		Descriptor descriptor = result.getResultDescriptor();
		@SuppressWarnings("unchecked")
		Object object = descriptor.serialize(result.getResult());

		return result.isSuccess() ? RpcResponses.ok(object) : RpcResponses.exception(object);
	}

	/** Creates an RPC error response from an unhandled exception. */
	public static RpcResponse writeError(final Exception e) {
		return RpcResponses.error(e);
	}

	/** Creates an RPC request reader. */
	public static FluentFunction<RpcRequest, Invocation> requestReader(
			final InterfaceDescriptor<?> descriptor) {
		return new RequestReader(descriptor);
	}

	/** Creates an RPC response writer. */
	public static FluentFunction<InvocationResult, RpcResponse> responseWriter() {
		return new ResponseWriter();
	}

	/** Creates an RPC error handler. */
	public static FluentFunction<Exception, RpcResponse> errorHandler() {
		return new ErrorHandler();
	}

	private static class RequestReader extends FluentFunction<RpcRequest, Invocation> {
		private final InterfaceDescriptor<?> descriptor;

		private RequestReader(final InterfaceDescriptor<?> descriptor) {
			this.descriptor = checkNotNull(descriptor);
		}

		@Override
		public Invocation apply(final RpcRequest input) {
			return readRequest(input, descriptor);
		}
	}

	private static class ResponseWriter extends FluentFunction<InvocationResult, RpcResponse> {
		@Override
		public RpcResponse apply(final InvocationResult input) {
			return writeResponse(input);
		}
	}

	private static class ErrorHandler extends FluentFunction<Exception, RpcResponse> {
		@Override
		public RpcResponse apply(final Exception input) {
			return writeError(input);
		}
	}
}
