package io.pdef.rpc;

public class RpcErrors {
	private RpcErrors() {}

	public static RpcError badRequest() {
		return RpcError.builder()
				.setCode(RpcErrorCode.BAD_REQUEST)
				.setText("Failed to parse the request")
				.build();
	}

	public static RpcError methodCallsRequired() {
		return RpcError.builder()
				.setCode(RpcErrorCode.BAD_REQUEST)
				.setText("Method calls required.")
				.build();
	}

	public static RpcError methodNotFound(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.BAD_REQUEST)
				.setText("Method not found: '" + path + "'")
				.build();
	}

	public static RpcError wrongMethodArgs(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.BAD_REQUEST)
				.setText("Wrong method arguments: '" + path + "'")
				.build();
	}

	public static RpcError notRemoteMethod(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.BAD_REQUEST)
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
