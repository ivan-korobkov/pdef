package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import pdef.Interface;
import pdef.MethodDescriptor;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import java.util.Map;

final class ParameterizedMethodDescriptor implements MethodDescriptor {
	private final MethodDescriptor raw;
	private final Map<String, TypeDescriptor> args;

	ParameterizedMethodDescriptor(final MethodDescriptor raw, final Map<String, TypeDescriptor> args) {
		this.raw = checkNotNull(raw);
		this.args = ImmutableMap.copyOf(args);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(raw.getName())
				.addValue(args)
				.toString();
	}

	@Override
	public String getName() {
		return raw.getName();
	}

	@Override
	public Map<String, TypeDescriptor> getArgs() {
		return args;
	}

	@Override
	public Object call(final Interface iface, final Map<String, Object> args) {
		return raw.call(iface, args);
	}

	@Override
	public MethodDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		Map<String, TypeDescriptor> parameterized = Maps.newLinkedHashMap();
		for (Map.Entry<String, TypeDescriptor> entry : args.entrySet()) {
			TypeDescriptor barg = entry.getValue().bind(argMap);
			parameterized.put(entry.getKey(), barg);
		}
		if (parameterized.equals(args)) return this;
		return new ParameterizedMethodDescriptor(raw, parameterized);
	}
}
