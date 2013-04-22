package io.pdef.rpc;

public interface Func<I, O> {

	O handle(I in);
}
