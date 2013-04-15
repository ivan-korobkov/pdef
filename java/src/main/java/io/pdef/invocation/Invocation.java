package io.pdef.invocation;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import io.pdef.descriptors.MethodDescriptor;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Invocation {
	private final MethodDescriptor method;
	private final List<?> args;

	public Invocation(final MethodDescriptor method, final List<?> args) {
		this.method = checkNotNull(method);
		this.args = ImmutableList.copyOf(args);
	}

	public MethodDescriptor getMethod() {
		return method;
	}

	public List<?> getArgs() {
		return args;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(method)
				.addValue(args)
				.toString();
	}
}
