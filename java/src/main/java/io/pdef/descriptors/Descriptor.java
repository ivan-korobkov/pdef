package io.pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;

/**
 * Descriptor is a base class for Pdef descriptors. Descriptors provides information about
 * a Java type in runtime. For example, a message descriptor allows to explore declared fields,
 * inherited fields, etc.
 * */
public class Descriptor<T> {
	private final TypeEnum type;
	private final Class<T> javaClass;

	protected Descriptor(final TypeEnum type, final Class<T> javaClass) {
		this.type = checkNotNull(type);
		this.javaClass = checkNotNull(javaClass);
	}

	/** Returns a type. */
	public TypeEnum getType() {
		return type;
	}

	/** Returns a java class. */
	public Class<T> getJavaClass() {
		return javaClass;
	}

	/** Casts a class to this descriptor java class. */
	public T cast(final Object data) {
		return javaClass.cast(data);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(type)
				.toString();
	}
}
