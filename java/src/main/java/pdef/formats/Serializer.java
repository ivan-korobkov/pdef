package pdef.formats;

import pdef.Message;

public interface Serializer {

	Object serialize(Message message);
}
