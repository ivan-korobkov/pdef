package io.pdef.rpc;

public class RpcResponses {
	private RpcResponses() {}

	/** Creates a successful RPC response, the result must be a serialized object. */
	public static RpcResponse ok(final Object result) {
		return RpcResponse.builder()
				.setStatus(RpcResponseStatus.OK)
				.setResult(result)
				.build();
	}

	/** Creates an exceptional RPC response, the exc must be a serialized object. */
	public static RpcResponse exception(final Object exc) {
		return RpcResponse.builder()
				.setStatus(RpcResponseStatus.EXCEPTION)
				.setResult(exc)
				.build();
	}

	/** Creates an error RPC response. */
	public static RpcResponse error(final Exception e) {
		RpcError error = RpcErrors.fromException(e);
		return error(error);
	}

	/** Creates an error RPC response. */
	public static RpcResponse error(final RpcError error) {
		Object result = error.serialize();
		return RpcResponse.builder()
				.setStatus(RpcResponseStatus.ERROR)
				.setResult(result)
				.build();
	}
}
