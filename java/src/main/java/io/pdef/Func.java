package io.pdef;

public interface Func<In, Out> {
	Out apply(In in) throws Exception;
}
