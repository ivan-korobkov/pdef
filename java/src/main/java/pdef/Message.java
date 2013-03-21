package pdef;

public interface Message extends Type {

	@Override
	MessageDescriptor getDescriptor();

	Builder newBuilder();

	Builder toBuilder();

	interface Builder {
		MessageDescriptor getDescriptor();

		Message build();
	}
}
