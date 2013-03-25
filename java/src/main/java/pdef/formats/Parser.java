package pdef.formats;

import pdef.MessageDescriptor;

public interface Parser {

	Object parse(MessageDescriptor descriptor, final Object object);
}
