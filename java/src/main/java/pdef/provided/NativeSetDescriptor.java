package pdef.provided;

import static com.google.common.base.Preconditions.*;
import pdef.ImmutableSymbolTable;
import pdef.SymbolTable;
import pdef.descriptors.SetDescriptor;
import pdef.descriptors.TypeDescriptor;
import pdef.descriptors.VariableDescriptor;

import java.util.Arrays;
import java.util.Map;

public final class NativeSetDescriptor implements SetDescriptor, NativeDescriptor {
	private static final NativeSetDescriptor INSTANCE = new NativeSetDescriptor();

	public static NativeSetDescriptor getInstance() {
		return INSTANCE;
	}

	private final VariableDescriptor element;
	private final SymbolTable<VariableDescriptor> variables;

	private NativeSetDescriptor() {
		element = new NativeVariableDescriptor("T");
		variables = ImmutableSymbolTable.of(element);
	}

	@Override
	public TypeDescriptor getElement() { return element; }

	@Override
	public SymbolTable<VariableDescriptor> getVariables() { return variables; }

	@Override
	public SetDescriptor parameterize(final TypeDescriptor... args) {
		checkArgument(args.length == 1, "Wrong number of arguments for %s: %s", this,
				Arrays.toString(args));
		return new Parameterized(args[0]);
	}

	@Override
	public TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap) { return this; }

	class Parameterized implements SetDescriptor {
		private final TypeDescriptor element;

		Parameterized(final TypeDescriptor element) { this.element = element; }

		@Override
		public TypeDescriptor getElement() { return element; }

		@Override
		public SymbolTable<VariableDescriptor> getVariables() { return ImmutableSymbolTable.of(); }

		@Override
		public SetDescriptor parameterize(final TypeDescriptor... args) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
			TypeDescriptor barg = element.bind(argMap);
			return NativeSetDescriptor.this.parameterize(barg);
		}
	}
}
