package pdef.descriptors;

import pdef.SymbolTable;

public interface MessageDescriptor extends TypeDescriptor {

	MessageDescriptor getBase();

	SymbolTable<FieldDescriptor> getDeclaredFields();

	SymbolTable<FieldDescriptor> getFields();
}
