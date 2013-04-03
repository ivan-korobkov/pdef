package pdef.generated;

import com.google.common.collect.ImmutableList;
import pdef.*;

import java.util.List;
import java.util.Map;

public abstract class GeneratedInterfaceDescriptor extends GeneratedTypeDescriptor
		implements InterfaceDescriptor {
	protected GeneratedInterfaceDescriptor(final Class<?> type) {
		super(type);
	}

	@Override
	public List<InterfaceDescriptor> getBases() {
		return ImmutableList.of();
	}

	@Override
	public SymbolTable<VariableDescriptor> getVariables() {
		return ImmutableSymbolTable.of();
	}

	@Override
	public InterfaceDescriptor parameterize(final TypeDescriptor... args) {
		return (InterfaceDescriptor) super.parameterize(args);
	}

	@Override
	protected TypeDescriptor newParameterizedType(final List<TypeDescriptor> args) {
		return new ParameterizedInterfaceDescriptor(this, args);
	}

	@Override
	public InterfaceDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return this;
	}
}
