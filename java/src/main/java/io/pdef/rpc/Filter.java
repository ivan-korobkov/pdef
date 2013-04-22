package io.pdef.rpc;

public interface Filter<I, O, I1, O1> {

	O handle(I in, Func<I1, O1> next);
}
