package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import pdef.MethodDescriptor;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import java.util.Map;

public abstract class GeneratedMethodDescriptor implements MethodDescriptor {
	private final String name;
	private final TypeDescriptor result;
	private final Map<String, TypeDescriptor> args;

	public GeneratedMethodDescriptor(final String name, final TypeDescriptor result,
			final Map<? extends String, ? extends TypeDescriptor> args) {
		this.name = checkNotNull(name);
		this.result = checkNotNull(result);
		this.args = ImmutableMap.copyOf(args);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.addValue(args)
				.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	public TypeDescriptor getResult() {
		return result;
	}

	@Override
	public Map<String, TypeDescriptor> getArgs() {
		return args;
	}

	@Override
	public MethodDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		TypeDescriptor presult = result.bind(argMap);
		Map<String, TypeDescriptor> parameterized = Maps.newLinkedHashMap();
		for (Map.Entry<String, TypeDescriptor> entry : args.entrySet()) {
			TypeDescriptor barg = entry.getValue().bind(argMap);
			parameterized.put(entry.getKey(), barg);
		}
		if (presult.equals(result) && parameterized.equals(args)) {
			return this;
		}
		return new ParameterizedMethodDescriptor(this, presult, parameterized);
	}
}
