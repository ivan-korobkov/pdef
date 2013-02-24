package pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

public abstract class AbstractTypeDescriptor implements TypeDescriptor {
	private final Class<?> type;
	private volatile boolean linked;

	protected AbstractTypeDescriptor(final Class<?> type) {
		this.type = checkNotNull(type);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(type)
				.toString();
	}

	public Class<?> getType() {
		return type;
	}

	public boolean isLinked() {
		return linked;
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
