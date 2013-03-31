package pdef;

public interface Message extends DataType {

	@Override
	MessageDescriptor getDescriptor();

	Builder newBuilderForType();

	Builder toBuilder();

	interface Builder {
		MessageDescriptor getDescriptor();

		Message build();
	}
}
