package com.ivankorobkov.pdef.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.ivankorobkov.pdef.data.Message;
import com.ivankorobkov.pdef.data.MessageDescriptor;

import java.io.IOException;

public interface JsonFormat {
	void serialize(Message value, JsonGenerator jgen) throws IOException;

	Message deserialize(MessageDescriptor descriptor, JsonParser parser) throws IOException;

	String toJson(Message message) throws IOException;

	Object fromJson(MessageDescriptor descriptor, String s) throws IOException;

	Object fromJson(MessageDescriptor descriptor, JsonNode root) throws IOException;
}
