package pdef;

import javax.annotation.Nullable;
import java.util.Map;

public interface MessageDescriptor extends DataTypeDescriptor {

	/** Returns this message base. */
	MessageDescriptor getBase();

	/** Returns this message root tree or the base tree. */
	@Nullable
	MessageTree getTree();

	/** Returns this message base tree. */
	@Nullable
	MessageTree getBaseTree();

	/** Returns this message root tree. */
	@Nullable
	MessageTree getRootTree();

	/** Returns this message polymorphic type field. */
	@Nullable
	FieldDescriptor getTypeField();

	/** Returns this message subtype. */
	@Nullable
	MessageDescriptor getSubtype(Object object);

	/** Returns true if this message has subtypes. */
	boolean hasSubtypes();

	/** Returns the fields declared in this message. */
	SymbolTable<FieldDescriptor> getDeclaredFields();

	/** Returns all fields in this message (declared + from the super messages). */
	SymbolTable<FieldDescriptor> getFields();

	@Override
	MessageDescriptor parameterize(TypeDescriptor... args);

	@Override
	MessageDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);

	// Below is obsolete

	Message.Builder newBuilder();
}
