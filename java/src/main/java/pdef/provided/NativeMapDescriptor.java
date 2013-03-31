package pdef.provided;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.*;

import java.util.Arrays;
import java.util.Map;

public final class NativeMapDescriptor implements MapDescriptor, Native {
	private static final NativeMapDescriptor INSTANCE = new NativeMapDescriptor();

	public static NativeMapDescriptor getInstance() {
		return INSTANCE;
	}

	private final VariableDescriptor key;
	private final VariableDescriptor value;
	private final SymbolTable<VariableDescriptor> variables;

	private NativeMapDescriptor() {
		key = new NativeVariableDescriptor("K");
		value = new NativeVariableDescriptor("V");
		variables = ImmutableSymbolTable.of(key, value);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(variables)
				.toString();
	}

	@Override
	public SymbolTable<VariableDescriptor> getVariables() { return variables; }

	@Override
	public TypeDescriptor getKey() { return key; }

	@Override
	public TypeDescriptor getValue() { return value; }

	@Override
	public MapDescriptor parameterize(final TypeDescriptor... args) {
		checkArgument(args.length == 2, "Wrong number of arguments for %s: %s", this,
				Arrays.toString(args));
		return new ParameterizedMapDescriptor(args[0], args[1]);
	}

	@Override
	public DataTypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap) { return this; }

	class ParameterizedMapDescriptor implements MapDescriptor {
		private final TypeDescriptor key;
		private final TypeDescriptor value;

		ParameterizedMapDescriptor(final TypeDescriptor key, final TypeDescriptor value) {
			this.key = checkNotNull(key);
			this.value = checkNotNull(value);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.addValue(key)
					.addValue(value)
					.toString();
		}

		@Override
		public SymbolTable<VariableDescriptor> getVariables() { return ImmutableSymbolTable.of(); }

		@Override
		public TypeDescriptor getKey() { return key; }

		@Override
		public TypeDescriptor getValue() { return value; }

		@Override
		public MapDescriptor parameterize(final TypeDescriptor... args) {
			throw new UnsupportedOperationException();
		}

		@Override
		public DataTypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
			TypeDescriptor bkey = key.bind(argMap);
			TypeDescriptor bvalue = value.bind(argMap);
			return NativeMapDescriptor.this.parameterize(bkey, bvalue);
		}
	}
}
