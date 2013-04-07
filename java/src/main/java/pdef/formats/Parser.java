package pdef.formats;

import pdef.MessageDescriptor;
import pdef.TypeDescriptor;

public interface Parser {

	Object parse(TypeDescriptor descriptor, final Object object);
}
