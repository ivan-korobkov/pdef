package pdef.provided;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.ImmutableSymbolTable;
import pdef.SymbolTable;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import java.util.Map;

public class NativeVariableDescriptor implements VariableDescriptor, NativeDescriptor {
	private final String name;

	public NativeVariableDescriptor(final String name) { this.name = checkNotNull(name); }

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.toString();
	}

	@Override
	public String getName() { return name; }

	@Override
	public SymbolTable<VariableDescriptor> getVariables() { return ImmutableSymbolTable.of(); }

	@Override
	public TypeDescriptor parameterize(final TypeDescriptor... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		TypeDescriptor arg = argMap.get(this);
		checkState(arg != null, "Variable %s must be present in %s", this, argMap);
		return arg;
	}
}
