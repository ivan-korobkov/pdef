package pdef.descriptors;

import pdef.ImmutableSymbolTable;
import pdef.SymbolTable;

public abstract class AbstractMessageDescriptor
		extends AbstractTypeDescriptor implements MessageDescriptor {
	private ImmutableSymbolTable<FieldDescriptor> declaredFields;

	@Override
	public SymbolTable<FieldDescriptor> getDeclaredFields() {
		return declaredFields;
	}
	protected void setDeclaredFields(FieldDescriptor... fields) {
		declaredFields = ImmutableSymbolTable.of(fields);
	}
}
