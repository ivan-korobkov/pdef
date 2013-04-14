package pdef.provided;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.*;

import java.util.Arrays;
import java.util.Map;

public final class NativeListDescriptor implements ListDescriptor, Native {
	private static final NativeListDescriptor INSTANCE = new NativeListDescriptor();

	public static NativeListDescriptor getInstance() {
		return INSTANCE;
	}

	private final VariableDescriptor element;
	private final SymbolTable<VariableDescriptor> variables;

	private NativeListDescriptor() {
		element = new NativeVariableDescriptor("T");
		variables = ImmutableSymbolTable.of(element);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(variables)
				.toString();
	}

	@Override
	public TypeDescriptor getElement() { return element; }

	@Override
	public SymbolTable<VariableDescriptor> getVariables() { return variables; }

	@Override
	public ListDescriptor parameterize(final TypeDescriptor... args) {
		checkArgument(args.length == 1, "Wrong number of arguments for %s: %s", this,
				Arrays.toString(args));
		return new ParameterizedListDescriptor(args[0]);
	}

	@Override
	public TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap) { return this; }

	class ParameterizedListDescriptor implements ListDescriptor {
		private final TypeDescriptor element;

		ParameterizedListDescriptor(final TypeDescriptor element) { this.element = element; }

		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.addValue(element)
					.toString();
		}

		@Override
		public TypeDescriptor getElement() { return element; }

		@Override
		public SymbolTable<VariableDescriptor> getVariables() { return ImmutableSymbolTable.of(); }

		@Override
		public ListDescriptor parameterize(final TypeDescriptor... args) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
			TypeDescriptor barg = element.bind(argMap);
			return NativeListDescriptor.this.parameterize(barg);
		}
	}
}
