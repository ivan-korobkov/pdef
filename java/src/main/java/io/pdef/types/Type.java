package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

/** Base pdef type. */
public abstract class Type<T> {
	private final TypeEnum type;

	protected Type(final TypeEnum type) {
		this.type = checkNotNull(type);
	}

	/** Returns this type type. */
	public TypeEnum type() {
		return type;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(type)
				.toString();
	}
}
