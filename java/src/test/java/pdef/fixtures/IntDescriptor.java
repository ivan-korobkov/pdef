package pdef.fixtures;

import pdef.descriptors.AbstractTypeDescriptor;
import pdef.descriptors.ValueDescriptor;

public class IntDescriptor extends AbstractTypeDescriptor implements ValueDescriptor {
	private static final IntDescriptor INSTANCE = new IntDescriptor();

	public static IntDescriptor getInstance() {
		INSTANCE.link();
		return INSTANCE;
	}

	private IntDescriptor() {
		super(Integer.class);
	}

	@Override
	protected void doLink() {}
}
