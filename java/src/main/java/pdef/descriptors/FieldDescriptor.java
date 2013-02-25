package pdef.descriptors;

import pdef.Message;
import pdef.Symbol;

import java.util.Map;

public interface FieldDescriptor extends Symbol {

	TypeDescriptor getType();

	Object get(Message message);

	void set(Message message, Object value);

	FieldDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);
}
