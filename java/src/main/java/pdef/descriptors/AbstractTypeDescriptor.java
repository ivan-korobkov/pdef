package pdef.descriptors;

import com.google.common.base.Objects;

public abstract class AbstractTypeDescriptor implements TypeDescriptor {
	private volatile boolean linked;

	@Override
	public String toString() {
		return Objects.toStringHelper(this).toString();
	}

	@Override
	public void link() {
		if (linked) {
			return;
		}

		synchronized (TypeDescriptor.class) {
			if (linked) {
				return;
			}

			linked = true;
			doLink();
		}
	}

	protected abstract void doLink();
}
