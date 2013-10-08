package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

/** Data type type. */
public class Type {
	private final TypeEnum type;
	private final Class<?> javaClass;

	protected Type(final TypeEnum type, final Class<?> javaClass) {
		this.type = checkNotNull(type);
		this.javaClass = checkNotNull(javaClass);
	}

	/** Returns this type type. */
	public TypeEnum getType() {
		return type;
	}

	/** Returns this type Java class. */
	public Class<?> getJavaClass() {
		return javaClass;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(type)
				.toString();
	}
}
