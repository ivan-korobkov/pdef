package io.pdef.rpc;

public class RpcErrors {
	private RpcErrors() {}

	public static RpcError clientError(final String text) {
		return RpcError.builder()
				.setCode(RpcErrorCode.CLIENT_ERROR)
				.setText(text)
				.build();
	}

	public static RpcError badRequest() {
		return clientError("Failed to parse the request");
	}

	public static RpcError methodCallsRequired() {
		return clientError("Method calls required.");
	}

	public static RpcError methodNotFound(final CharSequence path) {
		return clientError("Method not found: " + path);
	}

	public static RpcError wrongMethodArgs(final CharSequence path) {
		return clientError("Wrong method arguments: " + path);
	}

	public static RpcError notRemoteMethod(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.CLIENT_ERROR)
				.setText("Not a remote method: '" + path + "'")
				.build();
	}

	public static RpcError fromException(final Exception e) {
		return e instanceof RpcError ? (RpcError) e : RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("Internal server error")
				.build();
	}
}
