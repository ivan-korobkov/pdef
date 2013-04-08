package pdef.formats;

import pdef.Message;
import pdef.TypeDescriptor;

public interface Serializer {

	Object serialize(Message message);

	Object serialize(TypeDescriptor type, Object object);
}
