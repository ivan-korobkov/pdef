package io.pdef;

import com.google.common.base.Function;

public interface Filter<In0, Out0, In1, Out1> {
	Out0 apply(In0 input, Function<In1, Out1> next);

	Function<In0, Out0> then(Function<In1, Out1> next);

	<In2, Out2> Filter<In0, Out0, In2, Out2> then(Filter<In1, Out1, In2, Out2> next);
}
