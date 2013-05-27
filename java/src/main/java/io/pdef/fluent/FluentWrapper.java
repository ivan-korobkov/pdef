package io.pdef.fluent;

public interface FluentWrapper<In, Out, In1, Out1> {
	FluentFuture<Out> apply(In in, FluentFunc<In1, Out1> next);
}
