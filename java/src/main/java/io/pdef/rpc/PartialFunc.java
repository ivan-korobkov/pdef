package io.pdef.rpc;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;

public class PartialFunc<I1, O1, I, O> implements Func<I1, O1> {
	private final Filter<I1, O1, I, O> filter;
	private final Func<I, O> func;

	public PartialFunc(final Filter<I1, O1, I, O> filter, final Func<I, O> func) {
		this.filter = checkNotNull(filter);
		this.func = checkNotNull(func);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper("PartialFunc")
				.addValue(filter)
				.addValue(func)
				.toString();
	}

	@Override
	public O1 handle(final I1 in) {
		return filter.handle(in, func);
	}
}
