package pdef.rpc;

import pdef.InterfaceDescriptor;

import java.util.List;

public interface RpcParser {

	List<Call> parse(InterfaceDescriptor descriptor, Object request);
}
