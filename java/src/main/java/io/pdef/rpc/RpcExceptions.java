package io.pdef.rpc;

public class RpcExceptions {
	private RpcExceptions() {}

	public static RpcException methodCallsRequired() {
		return RpcException.builder()
				.setCode(RpcExceptionCode.METHOD_CALLS_REQUIRED)
				.setText("Method calls requried.")
				.build();
	}

	public static RpcException methodNotFound(final CharSequence path) {
		return RpcException.builder()
				.setCode(RpcExceptionCode.METHOD_NOT_FOUND)
				.setText("Method not found: " + path)
				.build();
	}

	public static RpcException dataMethodCallRequired(final CharSequence path) {
		return RpcException.builder()
				.setCode(RpcExceptionCode.DATA_METHOD_CALL_REQUIRED)
				.setText("Data method call required: " + path)
				.build();
	}

	public static RpcException dataMethodReachedNoMoCalls(final CharSequence path) {
		return RpcException.builder()
				.setCode(RpcExceptionCode.DATA_METHOD_REACHED_NO_MORE_CALLS)
				.setText("Data method reached, cannot invoke any more methods: " + path)
				.build();
	}

	public static RpcException wrongNumberOfMethodArgs(final CharSequence path, final int expected,
			final int provided) {
		return RpcException.builder()
				.setCode(RpcExceptionCode.WRONG_METHOD_ARGUMENTS)
				.setText(String.format(
						"Wrong number of method arguments, %s expected, %s provided: %s",
						expected, provided, path))
				.build();
	}

	public static RpcException wrongMethodArgs(final CharSequence path) {
		return RpcException.builder()
				.setCode(RpcExceptionCode.WRONG_METHOD_ARGUMENTS)
				.setText("Wrong method arguments: " + path)
				.build();
	}
}
