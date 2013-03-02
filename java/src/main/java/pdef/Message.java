package pdef;

public interface Message extends Type {

	@Override
	MessageDescriptor getDescriptor();

	interface Builder {
		MessageDescriptor getDescriptor();

		Message build();
	}
}
