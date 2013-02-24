package pdef.descriptors;

import pdef.Message;
import pdef.Symbol;

public interface FieldDescriptor extends Symbol {

	TypeDescriptor getType();

	Object get(Message message);

	void set(Message message, Object value);
}
