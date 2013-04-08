package pdef.formats;

import pdef.Message;
import pdef.TypeDescriptor;
import pdef.rpc.Call;

import java.util.List;

public interface Serializer {

	Object serialize(Message message);

	Object serialize(TypeDescriptor type, Object object);

	Object serializeCalls(List<Call> calls);
}
