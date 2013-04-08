package pdef.formats;

import pdef.InterfaceDescriptor;
import pdef.TypeDescriptor;
import pdef.rpc.Call;

import java.util.List;

public interface Parser {

	Object parse(TypeDescriptor descriptor, Object object);

	List<Call> parseCalls(InterfaceDescriptor descriptor, Object object);
}
