package pdef;

import java.util.List;
import java.util.Map;

public interface InterfaceDescriptor extends TypeDescriptor {

	/** Returns this interfaces base interfaces. */
	List<InterfaceDescriptor> getBases();

	/** Returns the methods declared in this interface. */
	SymbolTable<MethodDescriptor> getDeclaredMethods();

	/** Returns all methods in this interface (declared + from the base interaces). */
	SymbolTable<MethodDescriptor> getMethods();

	@Override
	InterfaceDescriptor parameterize(TypeDescriptor... args);

	@Override
	InterfaceDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);
}
