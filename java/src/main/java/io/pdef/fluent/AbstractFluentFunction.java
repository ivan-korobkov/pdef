package io.pdef.fluent;

import com.google.common.base.Function;

public abstract class AbstractFluentFunction<In, Out> implements FluentFunction<In, Out> {

	@Override
	public <Out1> FluentFunction<In, Out1> then(final Function<Out, Out1> next) {
		return FluentFunctions.then(this, next);
	}

	@Override
	public FluentFunction<In, Out> onError(final Function<Exception, Out> next) {
		return FluentFunctions.onErrorReturn(this, next);
	}

	@Override
	public <In1, Out1> FluentFunction<In1, Out1> wrap(final Wrapper<In1, Out1, In, Out> wrapper) {
		return FluentFunctions.wrap(this, wrapper);
	}
}
