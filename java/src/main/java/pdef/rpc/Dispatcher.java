package pdef.rpc;

import pdef.InterfaceDescriptor;

import java.util.Map;

public interface Dispatcher {

	Object dispatch(InterfaceDescriptor descriptor, Object service, Map<String, Object> request);
}
