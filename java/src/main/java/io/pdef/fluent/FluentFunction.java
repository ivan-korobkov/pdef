package io.pdef.fluent;

import com.google.common.base.Function;

public interface FluentFunction<In, Out> extends Function<In, Out> {

	/** Returns a new function, which executes the next one after this one. */
	<Out1> FluentFunction<In, Out1> then(Function<Out, Out1> next);

	/** Returns a new function, which catches all exceptions of this one. */
	FluentFunction<In, Out> onError(Function<Exception, Out> next);

	/** Wraps this function and returns a new one, can change the signature. */
	<In1, Out1> FluentFunction<In1, Out1> wrap(Wrapper<In1, Out1, In, Out> wrapper);

	/** Decorates this function and returns a new one, does not change the signature. */
	FluentFunction<In, Out> decorate(Wrapper<In, Out, In, Out> decorator);
}
