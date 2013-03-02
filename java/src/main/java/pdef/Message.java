package pdef;

public interface Message extends Type {

	@Override
	MessageDescriptor getDescriptor();

	interface Builder {
		Message build();
	}
}
