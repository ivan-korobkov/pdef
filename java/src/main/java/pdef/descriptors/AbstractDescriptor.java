package pdef.descriptors;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractDescriptor implements Descriptor {
	protected final DescriptorType type;
	protected final DescriptorPool pool;
	private boolean linked;

	protected AbstractDescriptor(final DescriptorType type, final DescriptorPool pool) {
		this.type = checkNotNull(type);
		this.pool = checkNotNull(pool);
	}

	public DescriptorType getType() {
		return type;
	}

	public DescriptorPool getPool() {
		return pool;
	}

	@Override
	public void link() {
		if (linked) return;
		doLink();
		linked = true;
	}

	protected abstract void doLink();
}
