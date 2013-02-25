package pdef.fixtures;

import pdef.descriptors.AbstractTypeDescriptor;
import pdef.descriptors.NativeDescriptor;
import pdef.descriptors.TypeDescriptor;
import pdef.descriptors.VariableDescriptor;

import java.util.Map;

public class IntDescriptor extends AbstractTypeDescriptor implements NativeDescriptor {
	private static final IntDescriptor INSTANCE = new IntDescriptor();

	public static IntDescriptor getInstance() {
		INSTANCE.link();
		return INSTANCE;
	}

	IntDescriptor() {
		super(Integer.class);
	}

	@Override
	public TypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return this;
	}

	@Override
	protected void doLink() {}
}
