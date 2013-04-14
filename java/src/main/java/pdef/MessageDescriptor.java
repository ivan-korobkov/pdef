package pdef;

import javax.annotation.Nullable;
import java.util.Map;

public interface MessageDescriptor extends TypeDescriptor {

	/** Returns this message base. */
	@Nullable
	MessageDescriptor getBase();

	/** Returns this message root tree or the base tree. */
	@Nullable
	Subtypes getSubtypes();

	/** Returns the fields declared in this message. */
	SymbolTable<FieldDescriptor> getDeclaredFields();

	/** Returns all fields in this message (declared + from the super messages). */
	SymbolTable<FieldDescriptor> getFields();

	@Override
	MessageDescriptor parameterize(TypeDescriptor... args);

	@Override
	MessageDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);

	Message.Builder newBuilder();
}
