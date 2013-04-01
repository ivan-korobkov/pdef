package pdef.generated;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import pdef.*;

import java.util.List;
import java.util.Map;

abstract class ParameterizedTypeDescriptor<T extends TypeDescriptor>
		extends GeneratedTypeDescriptor {
	protected final T raw;
	protected final List<TypeDescriptor> args;

	protected ParameterizedTypeDescriptor(final Class<?> type, final T raw,
			final List<TypeDescriptor> args) {
		super(type);
		this.raw = checkNotNull(raw);
		this.args = ImmutableList.copyOf(args);
		checkArgument(args.size() == raw.getVariables().size());
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass())
				.addValue(args)
				.toString();
	}

	public T getRaw() {
		return raw;
	}

	@Override
	public SymbolTable<VariableDescriptor> getVariables() {
		return ImmutableSymbolTable.of();
	}

	@Override
	public TypeDescriptor parameterize(final TypeDescriptor... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		Function<TypeDescriptor, TypeDescriptor> bindArg = bindFunc(argMap);
		List<TypeDescriptor> bargs = Lists.transform(args, bindArg);
		return newParameterizedType(bargs);
	}

	protected Map<VariableDescriptor, TypeDescriptor> argMap() {
		ImmutableMap.Builder<VariableDescriptor, TypeDescriptor> builder =
				ImmutableMap.builder();
		List<VariableDescriptor> vars = raw.getVariables().list();

		for (int i = 0; i < vars.size(); i++) {
			VariableDescriptor var = vars.get(i);
			TypeDescriptor arg = args.get(i);
			builder.put(var, arg);
		}

		return builder.build();
	}
}
