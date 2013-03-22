package pdef;

import javax.annotation.Nullable;
import java.util.Map;

public interface MessageDescriptor extends TypeDescriptor {

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

	/** Returns the fields declared in this message. */
	SymbolTable<FieldDescriptor> getDeclaredFields();

	/** Returns all fields in this message (declared + from the super messages). */
	SymbolTable<FieldDescriptor> getFields();

	@Override
	MessageDescriptor parameterize(TypeDescriptor... args);

	@Override
	MessageDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);

	Message.Builder newBuilder();

	@Override
	Map<String, Object> serialize(Object object);

	@Override
	Message parse(Object object);

	MessageDescriptor parseDescriptorType(Map<?, ?> map);
}
