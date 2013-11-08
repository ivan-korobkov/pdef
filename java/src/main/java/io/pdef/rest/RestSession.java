package io.pdef.rest;

import io.pdef.descriptors.ValueDescriptor;

public interface RestSession {
	<T, E> T send(RestRequest request, ValueDescriptor<T> resultDescriptor,
			ValueDescriptor<E> excDescriptor) throws Exception;
}
