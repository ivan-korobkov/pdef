package pdef;

import java.util.Map;

public interface TypeDescriptor {

	SymbolTable<VariableDescriptor> getVariables();

	TypeDescriptor parameterize(TypeDescriptor... args);

	TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);

	Object serialize(Object object);

	Object parse(Object object);
}
