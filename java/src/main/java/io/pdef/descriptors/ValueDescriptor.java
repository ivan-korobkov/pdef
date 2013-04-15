package io.pdef.descriptors;

public class ValueDescriptor extends AbstractDescriptor {

	protected ValueDescriptor(final Class<?> valueClass, final DescriptorPool pool) {
		super(valueClass, DescriptorType.VALUE, pool);
	}

	@Override
	public Class<?> getJavaType() {
		return (Class<?>) super.getJavaType();
	}

	@Override
	protected void doLink() {}
}
