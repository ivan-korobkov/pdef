package pdef;

import java.util.Map;

public interface Message extends Type {

	@Override
	MessageDescriptor getDescriptor();

	Builder newBuilder();

	Builder toBuilder();

	Map<String, Object> serialize();

	interface Builder {
		MessageDescriptor getDescriptor();

		Message build();
	}
}
