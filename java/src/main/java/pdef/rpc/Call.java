package pdef.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableMap;
import pdef.MethodDescriptor;

import java.util.Map;

public class Call {
	private final MethodDescriptor method;
	private final Map<?, ?> args;

	public Call(final MethodDescriptor method, final Map<?, ?> args) {
		this.method = checkNotNull(method);
		this.args = ImmutableMap.copyOf(args);
	}

	public MethodDescriptor getMethod() {
		return method;
	}

	public Map<?, ?> getArgs() {
		return args;
	}
}
