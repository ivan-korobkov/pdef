package io.pdef.rpc;

public class Errors {
	private Errors() {}

	public static Error methodCallsRequired() {
		return Error.builder()
				.setCode(ErrorCode.METHOD_CALLS_REQUIRED)
				.setText("Method calls requried.")
				.build();
	}

	public static Error methodNotFound(final CharSequence path) {
		return Error.builder()
				.setCode(ErrorCode.METHOD_NOT_FOUND)
				.setText("Method not found: " + path)
				.build();
	}

	public static Error dataMethodCallRequired(final CharSequence path) {
		return Error.builder()
				.setCode(ErrorCode.DATA_METHOD_CALL_REQUIRED)
				.setText("Data method call required: " + path)
				.build();
	}

	public static Error dataMethodReachedNoMoCalls(final CharSequence path) {
		return Error.builder()
				.setCode(ErrorCode.DATA_METHOD_REACHED_NO_MORE_CALLS)
				.setText("Data method reached, cannot invoke any more methods: " + path)
				.build();
	}

	public static Error wrongNumberOfMethodArgs(final CharSequence path, final int expected,
			final int provided) {
		return Error.builder()
				.setCode(ErrorCode.WRONG_METHOD_ARGUMENTS)
				.setText(String.format(
						"Wrong number of method arguments, %s expected, %s provided: %s",
						expected, provided, path))
				.build();
	}

	public static Error wrongMethodArgs(final CharSequence path) {
		return Error.builder()
				.setCode(ErrorCode.WRONG_METHOD_ARGUMENTS)
				.setText("Wrong method arguments: " + path)
				.build();
	}
}
