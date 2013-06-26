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
				.setCode(RpcErrorCode.METHOD_CALLS_REQUIRED)
				.setText("Method calls requried.")
				.build();
	}

	public static RpcError methodNotFound(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.METHOD_NOT_FOUND)
				.setText("Method not found: " + path)
				.build();
	}

	public static RpcError dataMethodCallRequired(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.DATA_METHOD_CALL_REQUIRED)
				.setText("Data method call required: " + path)
				.build();
	}

	public static RpcError dataMethodReachedNoMoCalls(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.DATA_METHOD_REACHED_NO_MORE_CALLS)
				.setText("Data method reached, cannot invoke any more methods: " + path)
				.build();
	}

	public static RpcError wrongNumberOfMethodArgs(final CharSequence path, final int expected,
			final int provided) {
		return RpcError.builder()
				.setCode(RpcErrorCode.WRONG_METHOD_ARGUMENTS)
				.setText(String.format(
						"Wrong number of method arguments, %s expected, %s provided: %s",
						expected, provided, path))
				.build();
	}

	public static RpcError wrongMethodArgs(final CharSequence path) {
		return RpcError.builder()
				.setCode(RpcErrorCode.WRONG_METHOD_ARGUMENTS)
				.setText("Wrong method arguments: " + path)
				.build();
	}
}
