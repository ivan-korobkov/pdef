package pdef.generated;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import pdef.Interface;
import pdef.MethodDescriptor;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import java.util.List;
import java.util.Map;

final class ParameterizedMethodDescriptor implements MethodDescriptor {
	private final MethodDescriptor raw;
	private final List<TypeDescriptor> args;

	ParameterizedMethodDescriptor(final MethodDescriptor raw, final List<TypeDescriptor> args) {
		this.raw = checkNotNull(raw);
		this.args = ImmutableList.copyOf(args);
	}

	@Override
	public String getName() {
		return raw.getName();
	}

	@Override
	public List<TypeDescriptor> getArgs() {
		return args;
	}

	@Override
	public Object call(final Interface iface, final List<Object> args) {
		return raw.call(iface, args);
	}

	@Override
	public MethodDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		List<TypeDescriptor> parameterized = Lists.newArrayList();
		for (TypeDescriptor arg : args) {
			TypeDescriptor barg = arg.bind(argMap);
			parameterized.add(barg);
		}
		if (parameterized.equals(args)) return this;
		return new ParameterizedMethodDescriptor(raw, parameterized);
	}
}
