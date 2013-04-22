package io.pdef.rpc;

public interface Func<In, Out> {

	Out apply(In in);
}
