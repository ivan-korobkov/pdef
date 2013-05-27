package io.pdef.fluent;

import com.google.common.base.Function;
import com.google.common.base.Objects;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/** FluentFunc supports decorations and automatically wraps exceptions and null results into
 * fluent futures. */
public abstract class FluentFunc<In, Out> implements Function<In, FluentFuture<Out>> {
	private final FluentFunc<In, Out> that = this;

	public static <In, Out> FluentFunc<In, Out> of(final Function<In, FluentFuture<Out>> func) {
		checkNotNull(func);
		if (func instanceof FluentFunc) {
			return (FluentFunc<In, Out>) func;
		}

		return new FluentFunc<In, Out>() {
			@Override
			public String toString() {
				return Objects.toStringHelper(this)
						.addValue(func)
						.toString();
			}

			@Override
			protected FluentFuture<Out> doApply(final In in) {
				return func.apply(in);
			}
		};
	}

	@Nonnull
	public FluentFuture<Out> apply(In in) {
		try {
			FluentFuture<Out> future = doApply(in);
			return future != null ? future : FluentFutures.<Out>of(null);
		} catch (Exception e) {
			return FluentFutures.failed(e);
		}
	}

	protected abstract FluentFuture<Out> doApply(final In in);

	public <In1, Out1> FluentFunc<In1, Out1> wrap(
			final FluentWrapper<In1, Out1, In, Out> wrapper) {
		return new FluentFunc<In1, Out1>() {
			@Override
			protected FluentFuture<Out1> doApply(final In1 in1) {
				return wrapper.apply(in1, that);
			}
		};
	}
}
