package io.pdef.rpc;

import io.pdef.descriptors.ValueDescriptor;

public interface ClientSession {
	<T, E> T send(RpcRequest request, ValueDescriptor<T> resultDescriptor,
			ValueDescriptor<E> excDescriptor) throws Exception;
}
