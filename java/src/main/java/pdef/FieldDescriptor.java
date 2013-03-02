package pdef;

import java.util.Map;

public interface FieldDescriptor extends Symbol {

	TypeDescriptor getType();

	Object get(Message message);

	Object get(Message.Builder builder);

	void set(Message.Builder builder, Object value);

	FieldDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);
}
