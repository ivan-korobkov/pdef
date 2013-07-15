package io.pdef.func;

import com.google.common.base.Function;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class FluentFilter<In0, Out0, In1, Out1> implements Filter<In0, Out0, In1, Out1> {
	public static <In0, Out0, In1, Out1> FluentFilter<In0, Out0, In1, Out1> from(
			final Filter<In0, Out0, In1, Out1> filter) {
		checkNotNull(filter);
		return new FluentFilter<In0, Out0, In1, Out1>() {
			@Override
			public String toString() {
				return filter.toString();
			}

			@Override
			public Out0 apply(final In0 input, final Function<In1, Out1> next) {
				return filter.apply(input, next);
			}
		};
	}

	public Function<In0, Out0> then(final Function<In1, Out1> next) {
		return new Function<In0, Out0>() {
			@Override
			public String toString() {
				return next.toString();
			}

			@Override
			public Out0 apply(@Nullable final In0 input) {
				return FluentFilter.this.apply(input, next);
			}
		};
	}

	public <In2, Out2> FluentFilter<In0, Out0, In2, Out2> then(
			final Filter<In1, Out1, In2, Out2> next) {
		final FluentFilter<In0, Out0, In1, Out1> that = this;
		return new FluentFilter<In0, Out0, In2, Out2>() {
			@Override
			public String toString() {
				return next.toString();
			}

			@Override
			public Out0 apply(final In0 input, final Function<In2, Out2> next1) {
				return that.apply(input, new Function<In1, Out1>() {
					@Nullable
					@Override
					public Out1 apply(@Nullable final In1 input) {
						return next.apply(input, next1);
					}
				});
			}
		};
	}
}
