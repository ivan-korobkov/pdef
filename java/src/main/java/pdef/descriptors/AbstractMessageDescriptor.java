package pdef.descriptors;

public abstract class AbstractMessageDescriptor
		extends AbstractTypeDescriptor implements MessageDescriptor {

	protected AbstractMessageDescriptor(final Class<?> type) {
		super(type);
	}

	@Override
	public MessageDescriptor getBase() {
		return null;
	}
}
