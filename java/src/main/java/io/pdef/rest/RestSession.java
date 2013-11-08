package io.pdef.rest;

import io.pdef.descriptors.DataTypeDescriptor;

public interface RestSession {
	<T, E> T send(RestRequest request, DataTypeDescriptor<T> resultDescriptor,
			DataTypeDescriptor<E> excDescriptor) throws Exception;
}
