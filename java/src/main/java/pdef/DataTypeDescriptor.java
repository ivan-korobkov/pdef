package pdef;

public interface DataTypeDescriptor extends TypeDescriptor {

	SymbolTable<VariableDescriptor> getVariables();
}
