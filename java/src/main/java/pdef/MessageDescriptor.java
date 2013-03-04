package pdef;

import java.util.Map;

public interface MessageDescriptor extends TypeDescriptor {

	/**
	 * Returns this message base.
	 */
	MessageDescriptor getBase();

	/**
	 * Returns this message type in the base type tree.
	 */
	Enum<?> getBaseType();

	/**
	 * Returns the default type in this message type tree.
	 */
	Enum<?> getDefaultType();

	/**
	 * Returns the type field in this message type tree.
	 */
	FieldDescriptor getTypeField();

	/**
	 * Returns this message type tree.
	 */
	Map<Enum<?>, MessageDescriptor> getSubtypes();

	SymbolTable<FieldDescriptor> getDeclaredFields();

	SymbolTable<FieldDescriptor> getFields();

	@Override
	MessageDescriptor parameterize(TypeDescriptor... args);

	@Override
	MessageDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);
}
