package pdef.descriptors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ValueDescriptor extends AbstractDescriptor {
	private final Class<?> valueClass;

	protected ValueDescriptor(final Class<?> valueClass, final DescriptorPool pool) {
		super(DescriptorType.VALUE, pool);
		this.valueClass = checkNotNull(valueClass);
	}

	public Class<?> getValueClass() {
		return valueClass;
	}

	@Override
	protected void doLink() {}
}
