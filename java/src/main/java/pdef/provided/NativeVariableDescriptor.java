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

	@Override
	public Object serialize(final Object object) {
		throw new UnsupportedOperationException("Generic variables do not support serialization, "
				+ "parameterize a type to make it serializable. The variable is " + this + ", "
				+ "the object is " + object);
	}

	@Override
	public Object parse(final Object object) {
		return new UnsupportedOperationException("Generic variables do not support parsing, "
				+ "parameterize a type to make it parseable. The variable is " + this + ", "
				+ "the object is " + object);
	}
}
