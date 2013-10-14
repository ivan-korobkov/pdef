package io.pdef;

public interface Func<Arg, Result> {
	Result apply(Arg arg);
}
