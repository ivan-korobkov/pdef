package com.ivankorobkov.pdef.data;

public interface MessageFieldAccessor {
	Object get(Message message);

	void set(Message message, Object value);

	boolean isSetIn(Message message);

	void clear(Message message);
}
