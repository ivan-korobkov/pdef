package pdef;

public interface TypeDescriptor extends Bindable<TypeDescriptor> {

	SymbolTable<VariableDescriptor> getVariables();

	TypeDescriptor parameterize(TypeDescriptor... args);
}
