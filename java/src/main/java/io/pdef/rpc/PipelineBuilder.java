package io.pdef.rpc;

import static com.google.common.base.Preconditions.checkNotNull;

public class PipelineBuilder<I, O> {

	public static <I, O> PipelineBuilder<I, O> start(final Func<I, O> func) {
		return new PipelineBuilder<I, O>(func);
	}

	private final Func<I, O> func;

	private PipelineBuilder(final Func<I, O> func) {
		this.func = checkNotNull(func);
	}

	public <I1, O1> PipelineBuilder<I1, O1> add(final Filter<I1, O1, I, O> filter) {
		return start(new PartialFunc<I1, O1, I, O>(filter, func));
	}

	public Func<I, O> build() {
		return func;
	}
}
