package pdef.descriptors;

import pdef.SymbolTable;

public interface MessageDescriptor extends TypeDescriptor {

	SymbolTable<FieldDescriptor> getDeclaredFields();
}
