package pdef.descriptors;

import pdef.SymbolTable;

import java.util.Map;

public interface MessageDescriptor extends TypeDescriptor {

	SymbolTable<VariableDescriptor> getVariables();

	MessageDescriptor getBase();

	Enum<?> getType();

	Map<Enum<?>, MessageDescriptor> getTypeMap();

	SymbolTable<FieldDescriptor> getDeclaredFields();

	SymbolTable<FieldDescriptor> getFields();

	MessageDescriptor parameterize(TypeDescriptor... args);

	@Override
	MessageDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);
}
