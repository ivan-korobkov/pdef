package pdef;

import java.util.Map;

public interface TypeDescriptor {

	Object getDefaultInstance();

	SymbolTable<VariableDescriptor> getVariables();

	TypeDescriptor parameterize(TypeDescriptor... args);

	TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);
}
