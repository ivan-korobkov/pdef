package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.TypeDescriptor;

abstract class GeneratedTypeDescriptor implements TypeDescriptor, GeneratedDescriptor {
	private final Class<?> type;
	private volatile State state = State.NEW;

	protected GeneratedTypeDescriptor(final Class<?> type) {
		this.type = checkNotNull(type);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(type)
				.toString();
	}

	public Class<?> getJavaClass() {
		return type;
	}

	public boolean isLinked() {
		return state == State.LINKED;
	}

	@Override
	public void link() {
		if (state != State.NEW) {
			return;
		}

		synchronized (TypeDescriptor.class) {
			if (state != State.NEW) {
				return;
			}

			state = State.LINKING;
			doLink();
			state = State.LINKED;
		}
	}

	protected abstract void doLink();

	enum State {
		NEW, LINKING, LINKED
	}
}
