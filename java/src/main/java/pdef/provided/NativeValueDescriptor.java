package pdef.provided;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.*;

import java.util.Map;

public class NativeValueDescriptor implements ValueDescriptor {
	private final Class<?> javaClass;

	NativeValueDescriptor(final Class<?> javaClass) {
		this.javaClass = checkNotNull(javaClass);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(javaClass)
				.toString();
	}

	@Override
	public Class<?> getJavaClass() { return javaClass; }

	@Override
	public SymbolTable<VariableDescriptor> getVariables() { return ImmutableSymbolTable.of(); }

	@Override
	public TypeDescriptor parameterize(final TypeDescriptor... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DataTypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap) { return this; }
}
