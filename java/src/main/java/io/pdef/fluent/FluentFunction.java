package io.pdef.fluent;

import com.google.common.base.Function;

public interface FluentFunction<In, Out> extends Function<In, Out> {

	<Out1> FluentFunction<In, Out1> then(Function<Out, Out1> next);

	FluentFunction<In, Out> onError(Function<Exception, Out> next);

	<In1, Out1> FluentFunction<In1, Out1> wrap(Wrapper<In1, Out1, In, Out> wrapper);
}
