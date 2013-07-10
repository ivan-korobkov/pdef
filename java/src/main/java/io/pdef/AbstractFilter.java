package io.pdef;

import com.google.common.base.Function;

import javax.annotation.Nullable;

public abstract class AbstractFilter<In0, Out0, In1, Out1> implements Filter<In0, Out0, In1, Out1> {
	@Override
	public Function<In0, Out0> then(final Function<In1, Out1> next) {
		return new Function<In0, Out0>() {
			@Override
			public String toString() {
				return AbstractFilter.this.toString();
			}

			@Override
			public Out0 apply(@Nullable final In0 input) {
				return AbstractFilter.this.apply(input, next);
			}
		};
	}

	@Override
	public <In2, Out2> Filter<In0, Out0, In2, Out2> then(final Filter<In1, Out1, In2, Out2> next) {
		final AbstractFilter<In0, Out0, In1, Out1> that = this;
		return new AbstractFilter<In0, Out0, In2, Out2>() {
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
