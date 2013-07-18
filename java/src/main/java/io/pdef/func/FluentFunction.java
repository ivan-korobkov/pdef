package io.pdef.func;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

/** {@code FluentFunction} provides method to create function chains and error handlers. */
public abstract class FluentFunction<In, Out> implements Function<In, Out> {

	/** Creates a fluent function from a simple one. */
	public static <In, Out> FluentFunction<In, Out> from(final Function<In, Out> func) {
		if (func instanceof FluentFunction) return (FluentFunction<In, Out>) func;
		return new ForwardingFluentFunction<In, Out>(func);
	}

	/** Creates a function chain with a given one. */
	public <Out1> FluentFunction<In, Out1> then(final Function<Out, Out1> next) {
		return new FunctionChain<In, Out, Out1>(this, next);
	}

	/** Wraps this function into an error handler. */
	public FluentFunction<In, Out> onError(final Function<Exception, Out> errorHandler) {
		return new ErrorHandler<In, Out>(this, errorHandler);
	}

	/** Extracts a real function if it is a ForwardingFluentFunction to simplify stack traces. */
	@SuppressWarnings("unchecked")
	private static <In, Out> Function<In, Out> dereference(final Function<In, Out> func) {
		checkNotNull(func);
		return func instanceof ForwardingFluentFunction ? ((ForwardingFluentFunction) func).func
		                                                : func;
	}

	private static class ForwardingFluentFunction<In, Out> extends FluentFunction<In, Out> {
		private final Function<In, Out> func;

		private ForwardingFluentFunction(final Function<In, Out> func) {
			this.func = checkNotNull(func);
		}

		@Override
		public Out apply(final In input) {
			return func.apply(input);
		}
	}

	private static class FunctionChain<In, Out, Out1> extends FluentFunction<In, Out1> {
		private final Function<In, Out> func0;
		private final Function<Out, Out1> func1;

		private FunctionChain(final Function<In, Out> func0,
				final Function<Out, Out1> func1) {
			this.func0 = dereference(func0);
			this.func1 = dereference(func1);
		}

		@Override
		public Out1 apply(final In input) {
			Out out = func0.apply(input);
			return func1.apply(out);
		}
	}

	private static class ErrorHandler<In, Out> extends FluentFunction<In, Out> {
		private final Function<In, Out> func;
		private final Function<Exception, Out> errorHandler;

		private ErrorHandler(final Function<In, Out> func,
				final Function<Exception, Out> errorHandler) {
			this.func = dereference(func);
			this.errorHandler = dereference(errorHandler);
		}

		@Override
		public Out apply(@Nullable final In input) {
			try {
				return func.apply(input);
			} catch (Exception e) {
				return errorHandler.apply(e);
			}
		}
	}
}
