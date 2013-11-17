package io.pdef.rpc;

import io.pdef.descriptors.DataTypeDescriptor;

public interface RpcSession {
	<T, E> T send(RpcRequest request, DataTypeDescriptor<T> resultDescriptor,
			DataTypeDescriptor<E> excDescriptor) throws Exception;
}
