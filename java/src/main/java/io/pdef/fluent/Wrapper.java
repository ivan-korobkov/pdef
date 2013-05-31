package io.pdef.fluent;

import com.google.common.base.Function;

public interface Wrapper<In, Out, In1, Out1> {

	Out apply(In in, Function<In1, Out1> next);
}
