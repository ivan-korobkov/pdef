package io.pdef.func;

import com.google.common.base.Function;

/** Filter wraps one function call with another. Usually, a FluentFilter will be used,
 * which imitates function currying via FluentFilter#then. */
public interface Filter<In0, Out0, In1, Out1> {
	Out0 apply(In0 input, Function<In1, Out1> next);
}
