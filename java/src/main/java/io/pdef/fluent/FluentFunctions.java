package io.pdef.fluent;

import com.google.common.base.Function;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/** Fluent function utilities. */
public class FluentFunctions {
	private FluentFunctions() {}

	public static <In, Out> FluentFunction<In, Out> of(final Function<In, Out> func) {
		checkNotNull(func);

		if (func instanceof FluentFunction) return (FluentFunction<In, Out>) func;
		return new AbstractFluentFunction<In, Out>() {
			@Override
			public String toString() {
				return func.toString();
			}

			@Override
			public Out apply(final In input) {
				return func.apply(input);
			}
		};
	}

	public static <In, Out, Out1> FluentFunction<In, Out1> then(final Function<In, Out> func0,
			final Function<Out, Out1> func1) {
		return new FuncChain<In, Out, Out1>(func0, func1);
	}

	public static <In, Out> FluentFunction<In, Out> onErrorReturn(final Function<In, Out> func,
			final Function<Exception, Out> onErrorFunc) {
		return new ErrorHandler<In, Out>(func, onErrorFunc);
	}

	public static <In, Out, In1, Out1> FluentFunction<In, Out> wrap(final Function<In1, Out1> func,
			final Wrapper<In, Out, In1, Out1> wrapper) {
		return new WrapperFunc<In, Out, In1, Out1>(func, wrapper);
	}

	private static class FuncChain<In, Out, Out1> extends AbstractFluentFunction<In, Out1> {
		private final Function<In, Out> func0;
		private final Function<Out, Out1> func1;

		public FuncChain(final Function<In, Out> func0, final Function<Out, Out1> func1) {
			this.func0 = checkNotNull(func0);
			this.func1 = checkNotNull(func1);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper("FuncChain")
					.addValue(func0)
					.addValue(func1)
					.toString();
		}

		@Override
		public Out1 apply(final In input) {
			Out out = func0.apply(input);
			return func1.apply(out);
		}
	}

	private static class ErrorHandler<In, Out> extends AbstractFluentFunction<In, Out> {
		private final Function<In, Out> func;
		private final Function<Exception, Out> errorHandler;

		private ErrorHandler(final Function<In, Out> func,
				final Function<Exception, Out> errorHandler) {
			this.func = checkNotNull(func);
			this.errorHandler = checkNotNull(errorHandler);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper("ErrorHandler")
					.addValue(func)
					.addValue(errorHandler)
					.toString();
		}

		@Override
		public Out apply(final In input) {
			try {
				return func.apply(input);
			} catch (Exception e) {
				return errorHandler.apply(e);
			}
		}
	}

	private static class WrapperFunc<In, Out, In1, Out1> extends AbstractFluentFunction<In, Out> {
		private final Function<In1, Out1> func;
		private final Wrapper<In, Out, In1, Out1> wrapper;

		private WrapperFunc(final Function<In1, Out1> func,
				final Wrapper<In, Out, In1, Out1> wrapper) {
			this.func = checkNotNull(func);
			this.wrapper = checkNotNull(wrapper);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper("WrapperFunc")
					.addValue(wrapper)
					.addValue(func)
					.toString();
		}

		@Override
		public Out apply(final In input) {
			return wrapper.apply(input, func);
		}
	}
}
