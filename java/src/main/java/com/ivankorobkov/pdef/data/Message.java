package com.ivankorobkov.pdef.data;

public interface Message extends DataType {

	@Override
	MessageDescriptor getDescriptor();

	Message deepCopy();

	Message mergeFrom(Message other);

}
