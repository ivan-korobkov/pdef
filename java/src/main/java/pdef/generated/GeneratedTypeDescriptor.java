package pdef.generated;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import pdef.Bindable;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.List;
import java.util.Map;

abstract class GeneratedTypeDescriptor implements Generated, TypeDescriptor {
	enum State { NEW, LINKING, LINKED, INITIALIZING, INITIALIZED }

	private final Class<?> type;
	private final Map<List<TypeDescriptor>, TypeDescriptor> pmap;
	private volatile State state = State.NEW;

	protected GeneratedTypeDescriptor(final Class<?> type) {
		this.type = checkNotNull(type);
		pmap = Maps.newHashMap();
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
		if (state == State.INITIALIZED) return;
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

	@Override
	public TypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return this;
	}

	@Override
	public TypeDescriptor parameterize(final TypeDescriptor... args) {
		checkNotNull(args);
		checkArgument(getVariables().size() == args.length,
				"Wrong number of args for %s: %s", this, args);
		List<TypeDescriptor> argList = ImmutableList.copyOf(args);

		final TypeDescriptor parameterized;
		synchronized (this) {
			if (pmap.containsKey(argList)) {
				parameterized = pmap.get(argList);
			} else {
				parameterized = newParameterizedType(argList);
				pmap.put(argList, parameterized);
			}
		}

		if (parameterized instanceof Generated) ((Generated) parameterized).initialize();
		return parameterized;
	}

	protected abstract TypeDescriptor newParameterizedType(final List<TypeDescriptor> args);

	public static <T extends Bindable<T>> Function<T, T> bindFunc(
			final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return new Function<T, T>() {
			@Nullable
			@Override
			public T apply(@Nullable final T input) {
				if (input == null) return null;
				return input.bind(argMap);
			}
		};
	}
}
