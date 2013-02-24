package pdef.descriptors;

import pdef.ImmutableSymbolTable;
import pdef.SymbolTable;

public abstract class AbstractMessageDescriptor
		extends AbstractTypeDescriptor implements MessageDescriptor {

	protected AbstractMessageDescriptor(final Class<?> type) {
		super(type);
	}

	@Override
	public MessageDescriptor getBase() {
		return null;
	}

	@Override
	public SymbolTable<VariableDescriptor> getVariables() {
		return ImmutableSymbolTable.of();
	}

	@Override
	public MessageDescriptor parameterize(final TypeDescriptor... args) {
		throw new UnsupportedOperationException();
	}
}
