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
				.setText("Method not found: " + path)
				.build();
	}

	public static RpcError wrongMethodArgs(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.BAD_REQUEST)
				.setText("Wrong method arguments: " + path)
				.build();
	}

	public static RpcError notRemoteMethod(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.BAD_REQUEST)
				.setText(String.format("Must be a remote method, got %s", path))
				.build();
	}
}
