package pdef.descriptors;

import pdef.SymbolTable;

public interface MessageDescriptor extends TypeDescriptor {

	SymbolTable<VariableDescriptor> getVariables();

	MessageDescriptor getBase();

	SymbolTable<FieldDescriptor> getDeclaredFields();

	SymbolTable<FieldDescriptor> getFields();
}
