package io.pdef.rpc;

public abstract class Filter<In, Out, In1, Out1> {
	private final Filter<In,Out,In1,Out1> that = this;

	public abstract Out apply(final In request, final Func<In1, Out1> func);

	public Func<In, Out> then(final Func<In1, Out1> func) {
		return new Func<In, Out>() {
			@Override
			public Out apply(final In in) {
				return that.apply(in, func);
			}
		};
	}

	public <In2, Out2> Filter<In, Out, In2, Out2> then(
			final Filter<In1, Out1, In2, Out2> filter) {
		return new Filter<In, Out, In2, Out2>() {
			@Override
			public Out apply(final In request, final Func<In2, Out2> func) {
				return that.apply(request, new Func<In1, Out1>() {
					@Override
					public Out1 apply(final In1 in1) {
						return filter.apply(in1, func);
					}
				});
			}
		};
	}
}
