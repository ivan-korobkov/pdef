package pdef.descriptors;

public class ValueDescriptor extends AbstractDescriptor {

	protected ValueDescriptor(final Class<?> valueClass, final DescriptorPool pool) {
		super(valueClass, DescriptorType.VALUE, pool);
	}

	@Override
	protected void doLink() {}
}
