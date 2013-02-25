package pdef.descriptors;

import pdef.PdefMessage;
import pdef.Symbol;

import java.util.Map;

public interface FieldDescriptor extends Symbol {

	TypeDescriptor getType();

	Object get(PdefMessage message);

	void set(PdefMessage message, Object value);

	FieldDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap);
}
