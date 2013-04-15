package io.pdef.descriptors;

import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractDescriptor implements Descriptor {
	protected final Type javaType;
	protected final DescriptorType type;
	protected final DescriptorPool pool;
	private boolean linked;

	protected AbstractDescriptor(final Type javaType, final DescriptorType type,
			final DescriptorPool pool) {
		this.javaType = javaType;
		this.type = checkNotNull(type);
		this.pool = checkNotNull(pool);
	}

	public Type getJavaType() {
		return javaType;
	}

	public DescriptorType getType() {
		return type;
	}

	public DescriptorPool getPool() {
		return pool;
	}

	@Override
	public final void link() {
		if (linked) return;
		doLink();
		linked = true;
	}

	protected abstract void doLink();
}
