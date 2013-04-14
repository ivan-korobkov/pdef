package pdef;

public interface Message {

	MessageDescriptor getDescriptor();

	Builder newBuilderForType();

	Builder toBuilder();

	interface Builder {
		MessageDescriptor getDescriptor();

		Message build();
	}
}
