package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.DataTypeDescriptor;

import javax.annotation.concurrent.GuardedBy;

abstract class GeneratedTypeDescriptor implements DataTypeDescriptor, Generated {
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

	State getState() {
		return state;
	}

	@Override
	public void initialize() {
		if (state == State.INITIALIZED) {
			return;
		}

		GeneratedTypeInitializer.initialize(this);
	}

	@GuardedBy("externally")
	void executeLink() {
		checkState(state == State.NEW);
		state = State.LINKING;
		link();
		state = State.LINKED;
	}

	@GuardedBy("externally")
	void executeInit() {
		checkState(state == State.LINKED);
		state = State.INITIALIZING;
		init();
		state = State.INITIALIZED;
	}

	protected abstract void link();

	protected abstract void init();

	enum State {
		NEW, LINKING, LINKED, INITIALIZING, INITIALIZED
	}
}
