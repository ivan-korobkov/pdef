package com.ivankorobkov.pdef.json;

import com.ivankorobkov.pdef.data.Message;
import com.ivankorobkov.pdef.data.MessageDescriptor;

import java.io.IOException;

public interface LineFormat {
	String toJson(Message message);

	Message fromJson(MessageDescriptor descriptor, String s) throws IOException;
}
