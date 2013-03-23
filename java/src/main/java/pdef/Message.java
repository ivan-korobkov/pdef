package pdef;

import java.util.Map;

public interface Message extends Type {

	@Override
	MessageDescriptor getDescriptorForType();

	Builder newBuilderForType();

	Builder toBuilder();

	@Override
	Map<String, Object> serialize();

	interface Builder {
		MessageDescriptor getDescriptorForType();

		Message build();
	}
}
