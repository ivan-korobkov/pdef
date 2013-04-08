package pdef.rpc;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import pdef.MethodDescriptor;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Call {
	private final MethodDescriptor method;
	private final Map<?, ?> args;

	public Call(final MethodDescriptor method, final Map<?, ?> args) {
		this.method = checkNotNull(method);
		this.args = ImmutableMap.copyOf(args);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(method.getName())
				.addValue(args)
				.toString();
	}

	public MethodDescriptor getMethod() {
		return method;
	}

	public Map<?, ?> getArgs() {
		return args;
	}
}
